import { test } from 'node:test';
import assert from 'node:assert/strict';
import { LogStore } from './logStore.js';

test('stores logs and returns them newest first', () => {
  const store = new LogStore();
  store.add({ service: 'a', level: 'INFO', message: 'first' });
  store.add({ service: 'a', level: 'ERROR', message: 'second' });
  const logs = store.list();
  assert.equal(logs[0].message, 'second');
  assert.equal(logs.length, 2);
});

test('filters by level, service and text', () => {
  const store = new LogStore();
  store.add({ service: 'a', level: 'INFO', message: 'user logged' });
  store.add({ service: 'b', level: 'ERROR', message: 'db timeout' });
  assert.equal(store.list({ level: 'ERROR' }).length, 1);
  assert.equal(store.list({ service: 'a' }).length, 1);
  assert.equal(store.list({ search: 'TIMEOUT' }).length, 1);
});

test('drops oldest entries beyond capacity', () => {
  const store = new LogStore(2);
  store.add({ service: 'a', level: 'INFO', message: '1' });
  store.add({ service: 'a', level: 'INFO', message: '2' });
  store.add({ service: 'a', level: 'INFO', message: '3' });
  assert.deepEqual(store.list().map((l) => l.message), ['3', '2']);
});

test('computes stats', () => {
  const store = new LogStore();
  store.add({ service: 'a', level: 'INFO', message: '1' });
  store.add({ service: 'b', level: 'WARN', message: '2' });
  const stats = store.stats();
  assert.equal(stats.total, 2);
  assert.equal(stats.byLevel.WARN, 1);
  assert.equal(stats.byService.a, 1);
});
