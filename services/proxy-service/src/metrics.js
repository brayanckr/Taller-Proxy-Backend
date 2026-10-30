export class ProxyMetrics {
  startedAt = new Date().toISOString();
  counters = {
    totalRequests: 0,
    forwarded: 0,
    cacheHits: 0,
    cacheMisses: 0,
    unauthorized: 0,
    forbidden: 0,
    rejected: 0,
    rateLimited: 0,
    redactions: 0,
    buffered: 0,
    flushed: 0,
    dropped: 0,
  };

  increment(name, amount = 1) {
    this.counters[name] += amount;
  }

  snapshot(extra = {}) {
    return { startedAt: this.startedAt, ...this.counters, ...extra };
  }
}
