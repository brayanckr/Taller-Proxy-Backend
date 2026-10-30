# Taller Proxy - Backend

Microservices architecture that handles the logs emitted by business microservices using the **Proxy pattern**.

```
 auth-service ─┐
payments-service ─┼──(API key)──▶  proxy-service :4000  ──(internal token)──▶  log-service :4001
 orders-service ─┘                  (public entry point)                        (real subject, private)
        dashboard (frontend) ──────▶  reads logs, stats and metrics
```

## Services

| Service | Port | Responsibility |
| --- | --- | --- |
| `log-service` | 4001 | Real subject. Stores and queries logs in memory. Only accepts calls carrying the internal token. |
| `proxy-service` | 4000 | Proxy. The only public entry point. Same operations as the real service plus extra control. |
| `producer-simulator` | - | Optional. Simulates microservices that send logs through the proxy. |

## What the proxy adds (`LogServiceProxy`)

- **Authentication**: every request needs an `x-api-key`. Each key maps to a client and a role (`producer` writes, `viewer` reads).
- **Access control**: producers cannot read logs; viewers cannot write. A producer cannot send logs on behalf of another service.
- **Validation**: level, message length and timestamp are checked before reaching the log service.
- **Redaction**: values of keys such as `password`, `token`, `apiKey`, and `password=...` patterns in messages are masked as `[REDACTED]`.
- **Rate limiting**: fixed window per client (default 120 requests/minute).
- **Caching**: read queries are cached for a few seconds and invalidated after every write.
- **Buffering**: if the log service is down, writes are queued (`202 Accepted`) and flushed when it comes back.
- **Metrics**: `GET /api/metrics` exposes counters for every behavior above.

## API (through the proxy)

| Method | Path | Role | Description |
| --- | --- | --- | --- |
| POST | `/api/logs` | producer | `{ "level": "ERROR", "message": "...", "metadata": {} }` |
| GET | `/api/logs?level=&service=&search=&limit=` | viewer | List logs, newest first |
| GET | `/api/logs/stats` | viewer | Totals by level and by service |
| GET | `/api/metrics` | viewer | Proxy counters |
| GET | `/api/health` | none | Proxy and log-service status |

Default demo keys: `producer-key-auth`, `producer-key-payments`, `producer-key-orders` (producers) and `dashboard-key` (viewer). Override them with the `API_KEYS` environment variable (JSON map).

## Run

```bash
npm install
npm start                 # log-service + proxy-service
SIMULATOR=true npm start  # also start the log producer simulator
npm test
```

Or with Docker:

```bash
docker compose up --build
docker compose --profile simulator up --build
```

Example:

```bash
curl -X POST http://localhost:4000/api/logs \
  -H "x-api-key: producer-key-payments" -H "content-type: application/json" \
  -d '{"level":"ERROR","message":"Payment gateway timeout"}'

curl http://localhost:4000/api/logs -H "x-api-key: dashboard-key"
```

## Configuration

| Variable | Default |
| --- | --- |
| `PORT` | 4000 (proxy), 4001 (log-service) |
| `LOG_SERVICE_URL` | `http://localhost:4001` |
| `INTERNAL_TOKEN` | `internal-secret` |
| `API_KEYS` | demo keys above |
| `RATE_LIMIT` / `RATE_WINDOW_MS` | 120 / 60000 |
| `CACHE_TTL_MS` | 3000 |
