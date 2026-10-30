import cors from 'cors';
import express from 'express';
import { ProxyError } from './errors.js';

export function createApp({ proxy, clients, metrics }) {
  const app = express();
  app.use(cors());
  app.use(express.json({ limit: '100kb' }));

  app.get('/api/health', async (_req, res) => {
    const logServiceUp = await proxy.realService.isHealthy();
    res.json({ status: logServiceUp ? 'ok' : 'degraded', proxy: 'up', logService: logServiceUp ? 'up' : 'down' });
  });

  app.use('/api', (req, res, next) => {
    metrics.increment('totalRequests');
    const client = clients[req.get('x-api-key')];
    if (!client) {
      metrics.increment('unauthorized');
      return res.status(401).json({ error: 'unauthorized', message: 'Missing or invalid API key' });
    }
    req.client = client;
    next();
  });

  const handle = (fn) => (req, res, next) => fn(req, res).catch(next);

  app.post('/api/logs', handle(async (req, res) => {
    const result = await proxy.createLog(req.client, req.body);
    res.status(result.status === 'stored' ? 201 : 202).json(result);
  }));

  app.get('/api/logs', handle(async (req, res) => {
    const { level, service, search, limit } = req.query;
    res.json(await proxy.listLogs(req.client, { level, service, search, limit }));
  }));

  app.get('/api/logs/stats', handle(async (req, res) => {
    res.json(await proxy.getStats(req.client));
  }));

  app.get('/api/metrics', (req, res) => {
    res.json(proxy.getMetrics(req.client));
  });

  // eslint-disable-next-line no-unused-vars
  app.use((error, _req, res, _next) => {
    if (error instanceof ProxyError) {
      return res.status(error.status).json({ error: error.code, message: error.message });
    }
    if (error.type === 'entity.parse.failed') {
      metrics.increment('rejected');
      return res.status(400).json({ error: 'invalid_json', message: 'Body is not valid JSON' });
    }
    console.error(error);
    res.status(500).json({ error: 'internal_error', message: 'Unexpected error' });
  });

  return app;
}
