import express from 'express';
import { LEVELS, LogStore } from './logStore.js';

/**
 * Real subject of the Proxy pattern: stores and queries logs.
 * It is not exposed publicly; every request must carry the internal token
 * that only the proxy-service knows.
 */
export function createApp({ store = new LogStore(), internalToken }) {
  const app = express();
  app.use(express.json({ limit: '100kb' }));

  app.get('/health', (_req, res) => {
    res.json({ status: 'ok', service: 'log-service' });
  });

  app.use((req, res, next) => {
    if (internalToken && req.get('x-internal-token') !== internalToken) {
      return res.status(403).json({ error: 'forbidden', message: 'Only the proxy may call this service' });
    }
    next();
  });

  app.post('/logs', (req, res) => {
    const { service, level, message } = req.body ?? {};
    if (typeof service !== 'string' || !service || typeof message !== 'string' || !message) {
      return res.status(400).json({ error: 'invalid_log', message: 'service and message are required' });
    }
    if (!LEVELS.includes(level)) {
      return res.status(400).json({ error: 'invalid_log', message: `level must be one of ${LEVELS.join(', ')}` });
    }
    res.status(201).json(store.add(req.body));
  });

  app.get('/logs', (req, res) => {
    const { level, service, search } = req.query;
    const limit = Math.min(Number.parseInt(req.query.limit, 10) || 100, 1000);
    res.json(store.list({ level, service, search, limit }));
  });

  app.get('/logs/stats', (_req, res) => {
    res.json(store.stats());
  });

  app.delete('/logs', (_req, res) => {
    store.clear();
    res.status(204).end();
  });

  return app;
}
