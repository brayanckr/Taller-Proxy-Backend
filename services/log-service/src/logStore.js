import { randomUUID } from 'node:crypto';

export const LEVELS = ['DEBUG', 'INFO', 'WARN', 'ERROR', 'FATAL'];

/** In-memory ring buffer of log entries. */
export class LogStore {
  #entries = [];

  constructor(capacity = 5000) {
    this.capacity = capacity;
  }

  add({ service, level, message, timestamp, metadata }) {
    const entry = {
      id: randomUUID(),
      service,
      level,
      message,
      metadata: metadata ?? {},
      timestamp: timestamp ?? new Date().toISOString(),
      receivedAt: new Date().toISOString(),
    };
    this.#entries.push(entry);
    if (this.#entries.length > this.capacity) {
      this.#entries.shift();
    }
    return entry;
  }

  list({ level, service, search, limit = 100 } = {}) {
    const needle = search?.toLowerCase();
    const filtered = this.#entries.filter((entry) => {
      if (level && entry.level !== level) return false;
      if (service && entry.service !== service) return false;
      if (needle && !entry.message.toLowerCase().includes(needle)) return false;
      return true;
    });
    return filtered.slice(-limit).reverse();
  }

  stats() {
    const byLevel = Object.fromEntries(LEVELS.map((level) => [level, 0]));
    const byService = {};
    for (const entry of this.#entries) {
      byLevel[entry.level] += 1;
      byService[entry.service] = (byService[entry.service] ?? 0) + 1;
    }
    return { total: this.#entries.length, byLevel, byService };
  }

  clear() {
    this.#entries = [];
  }
}
