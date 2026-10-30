const DEFAULT_CLIENTS = {
  'producer-key-auth': { name: 'auth-service', role: 'producer' },
  'producer-key-payments': { name: 'payments-service', role: 'producer' },
  'producer-key-orders': { name: 'orders-service', role: 'producer' },
  'dashboard-key': { name: 'dashboard', role: 'viewer' },
};

export function loadConfig(env = process.env) {
  return {
    port: Number(env.PORT) || 4000,
    logServiceUrl: env.LOG_SERVICE_URL || 'http://localhost:4001',
    internalToken: env.INTERNAL_TOKEN || 'internal-secret',
    clients: env.API_KEYS ? JSON.parse(env.API_KEYS) : DEFAULT_CLIENTS,
    rateLimit: {
      limit: Number(env.RATE_LIMIT) || 120,
      windowMs: Number(env.RATE_WINDOW_MS) || 60_000,
    },
    cacheTtlMs: Number(env.CACHE_TTL_MS) || 3000,
    bufferCapacity: Number(env.BUFFER_CAPACITY) || 1000,
    flushIntervalMs: Number(env.FLUSH_INTERVAL_MS) || 2000,
  };
}
