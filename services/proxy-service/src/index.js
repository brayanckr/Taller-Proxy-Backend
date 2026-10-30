import { createApp } from './app.js';
import { TtlCache } from './cache.js';
import { loadConfig } from './config.js';
import { HttpLogService } from './logServiceClient.js';
import { LogServiceProxy } from './logServiceProxy.js';
import { ProxyMetrics } from './metrics.js';
import { RateLimiter } from './rateLimiter.js';

const config = loadConfig();
const metrics = new ProxyMetrics();

const proxy = new LogServiceProxy({
  realService: new HttpLogService({ baseUrl: config.logServiceUrl, internalToken: config.internalToken }),
  cache: new TtlCache({ ttlMs: config.cacheTtlMs }),
  rateLimiter: new RateLimiter(config.rateLimit),
  metrics,
  bufferCapacity: config.bufferCapacity,
});

setInterval(() => proxy.flushBuffer(), config.flushIntervalMs).unref();

createApp({ proxy, clients: config.clients, metrics }).listen(config.port, () => {
  console.log(`proxy-service listening on port ${config.port}, forwarding to ${config.logServiceUrl}`);
});
