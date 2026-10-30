import { ProxyError } from './errors.js';
import { buildLogEntry } from './validation.js';

/**
 * Proxy pattern: exposes the same operations as the real log service
 * (createLog, listLogs, getStats) but adds, in front of it:
 *  - access control by role
 *  - rate limiting per client
 *  - validation and redaction of sensitive data
 *  - caching of read queries
 *  - buffering of writes while the real service is down
 */
export class LogServiceProxy {
  #buffer = [];

  constructor({ realService, cache, rateLimiter, metrics, bufferCapacity = 1000 }) {
    this.realService = realService;
    this.cache = cache;
    this.rateLimiter = rateLimiter;
    this.metrics = metrics;
    this.bufferCapacity = bufferCapacity;
  }

  async createLog(client, input) {
    this.#authorize(client, 'producer');
    this.#throttle(client);

    let built;
    try {
      built = buildLogEntry(client, input);
    } catch (error) {
      this.metrics.increment(error.status === 403 ? 'forbidden' : 'rejected');
      throw error;
    }
    this.metrics.increment('redactions', built.redactions);

    try {
      const stored = await this.realService.createLog(built.entry);
      this.metrics.increment('forwarded');
      this.cache.clear();
      return { status: 'stored', log: stored };
    } catch (error) {
      if (error.code !== 'log_service_unavailable') throw error;
      this.#enqueue(built.entry);
      return { status: 'buffered', log: built.entry };
    }
  }

  async listLogs(client, filters = {}) {
    this.#authorize(client, 'viewer');
    this.#throttle(client);
    return this.#cached(`logs:${JSON.stringify(filters)}`, () => this.realService.listLogs(filters));
  }

  async getStats(client) {
    this.#authorize(client, 'viewer');
    this.#throttle(client);
    return this.#cached('stats', () => this.realService.getStats());
  }

  getMetrics(client) {
    this.#authorize(client, 'viewer');
    return this.metrics.snapshot({ pendingBuffer: this.#buffer.length });
  }

  /** Retries buffered logs in order; stops at the first failure. */
  async flushBuffer() {
    while (this.#buffer.length > 0) {
      try {
        await this.realService.createLog(this.#buffer[0]);
      } catch {
        return;
      }
      this.#buffer.shift();
      this.metrics.increment('flushed');
      this.cache.clear();
    }
  }

  get pendingBuffer() {
    return this.#buffer.length;
  }

  #enqueue(entry) {
    if (this.#buffer.length >= this.bufferCapacity) {
      this.#buffer.shift();
      this.metrics.increment('dropped');
    }
    this.#buffer.push(entry);
    this.metrics.increment('buffered');
  }

  async #cached(key, load) {
    const hit = this.cache.get(key);
    if (hit !== undefined) {
      this.metrics.increment('cacheHits');
      return hit;
    }
    this.metrics.increment('cacheMisses');
    const value = await load();
    this.metrics.increment('forwarded');
    this.cache.set(key, value);
    return value;
  }

  #authorize(client, requiredRole) {
    if (client.role !== requiredRole) {
      this.metrics.increment('forbidden');
      throw new ProxyError(403, 'forbidden', `This operation requires the "${requiredRole}" role`);
    }
  }

  #throttle(client) {
    if (!this.rateLimiter.consume(client.name)) {
      this.metrics.increment('rateLimited');
      throw new ProxyError(429, 'rate_limited', 'Too many requests, slow down');
    }
  }
}
