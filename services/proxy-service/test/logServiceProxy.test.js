import { test } from 'node:test';
import assert from 'node:assert/strict';
import { TtlCache } from '../src/cache.js';
import { LogServiceProxy } from '../src/logServiceProxy.js';
import { ProxyMetrics } from '../src/metrics.js';
import { RateLimiter } from '../src/rateLimiter.js';
import { ProxyError } from '../src/errors.js';

const producer = { name: 'auth-service', role: 'producer' };
const viewer = { name: 'dashboard', role: 'viewer' };

function setup({ limit = 100, failing = false } = {}) {
  const calls = { create: [], list: 0 };
  const state = { failing };
  const realService = {
    async createLog(entry) {
      if (state.failing) throw new ProxyError(503, 'log_service_unavailable', 'down');
      calls.create.push(entry);
      return { id: '1', ...entry };
    },
    async listLogs() {
      calls.list += 1;
      return [];
    },
    async getStats() {
      return { total: 0 };
    },
  };
  const metrics = new ProxyMetrics();
  const proxy = new LogServiceProxy({
    realService,
    cache: new TtlCache({ ttlMs: 10_000 }),
    rateLimiter: new RateLimiter({ limit, windowMs: 60_000 }),
    metrics,
  });
  return { proxy, calls, metrics, state };
}

test('forwards a valid log to the real service', async () => {
  const { proxy, calls } = setup();
  const result = await proxy.createLog(producer, { level: 'info', message: 'started' });
  assert.equal(result.status, 'stored');
  assert.equal(calls.create[0].service, 'auth-service');
  assert.equal(calls.create[0].level, 'INFO');
});

test('redacts sensitive data before forwarding', async () => {
  const { proxy, calls, metrics } = setup();
  await proxy.createLog(producer, {
    level: 'ERROR',
    message: 'login failed password=hunter2',
    metadata: { user: 'ana', apiKey: 'abc', nested: { token: 'xyz' } },
  });
  const sent = calls.create[0];
  assert.equal(sent.message, 'login failed password=[REDACTED]');
  assert.equal(sent.metadata.apiKey, '[REDACTED]');
  assert.equal(sent.metadata.nested.token, '[REDACTED]');
  assert.equal(sent.metadata.user, 'ana');
  assert.equal(metrics.counters.redactions, 3);
});

test('rejects invalid logs without calling the real service', async () => {
  const { proxy, calls, metrics } = setup();
  await assert.rejects(proxy.createLog(producer, { level: 'LOUD', message: 'x' }), { status: 400 });
  assert.equal(calls.create.length, 0);
  assert.equal(metrics.counters.rejected, 1);
});

test('blocks a producer from spoofing another service', async () => {
  const { proxy } = setup();
  await assert.rejects(proxy.createLog(producer, { level: 'INFO', message: 'x', service: 'payments-service' }), {
    status: 403,
    code: 'service_mismatch',
  });
});

test('enforces roles', async () => {
  const { proxy } = setup();
  await assert.rejects(proxy.createLog(viewer, { level: 'INFO', message: 'x' }), { status: 403 });
  await assert.rejects(proxy.listLogs(producer), { status: 403 });
});

test('rate limits a noisy client', async () => {
  const { proxy, metrics } = setup({ limit: 2 });
  await proxy.createLog(producer, { level: 'INFO', message: '1' });
  await proxy.createLog(producer, { level: 'INFO', message: '2' });
  await assert.rejects(proxy.createLog(producer, { level: 'INFO', message: '3' }), { status: 429 });
  assert.equal(metrics.counters.rateLimited, 1);
});

test('caches reads and invalidates the cache after a write', async () => {
  const { proxy, calls, metrics } = setup();
  await proxy.listLogs(viewer, { level: 'INFO' });
  await proxy.listLogs(viewer, { level: 'INFO' });
  assert.equal(calls.list, 1);
  assert.equal(metrics.counters.cacheHits, 1);

  await proxy.createLog(producer, { level: 'INFO', message: 'new' });
  await proxy.listLogs(viewer, { level: 'INFO' });
  assert.equal(calls.list, 2);
});

test('buffers logs while the real service is down and flushes them later', async () => {
  const { proxy, calls, state } = setup({ failing: true });
  const result = await proxy.createLog(producer, { level: 'WARN', message: 'during outage' });
  assert.equal(result.status, 'buffered');
  assert.equal(proxy.pendingBuffer, 1);

  state.failing = false;
  await proxy.flushBuffer();
  assert.equal(proxy.pendingBuffer, 0);
  assert.equal(calls.create.length, 1);
});
