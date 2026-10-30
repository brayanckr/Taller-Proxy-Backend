# Microservices monitoring with a Logging Proxy - Backend

Spring Boot backend of the workshop "Proxy pattern". A `LoggingProxy` sits in front of three microservices (Inventory, Orders and Payments) and records every call: start, duration, result and, on failure, the error with its stack trace. The metrics are exposed for the dashboard.

Frontend repository: https://github.com/brayanckr/Taller-Proxy-Frontend

```
Controller ──▶ LoggingProxy<T> ──▶ InventoryService | OrderService | PaymentService
                    │
                    └──▶ LogStore (in memory) ──▶ MetricsService ──▶ /api/metrics/*
```

## Design

- `MicroserviceProxy<T>`: generic interface with `T execute(String operation, Object... params)`. The real services and the proxy implement it.
- `InventoryService`, `OrderService`, `PaymentService`: real services with simulated latency. `PaymentService` fails 10% of the calls on purpose (`app.payment.failure-rate`).
- `LoggingProxy<T>`: wraps any `MicroserviceProxy<T>`. Logs the start of the call, then duration and result, and the error with a summarized stack trace if it fails. Each entry has `serviceId`, `operation`, `durationMs`, `status` (SUCCESS/ERROR), `requestId` (UUID) and `timestamp`, plus the input params and the response for the detail view.
- `ProxyConfig`: exposes each service only through its proxy, so controllers can never call a service without being logged.

## Endpoints

| Method | Path | Description |
| --- | --- | --- |
| POST | `/api/services/inventory/{operation}` | `list`, `check-stock {productId}`, `reserve {productId, quantity}` |
| POST | `/api/services/orders/{operation}` | `list`, `get {orderId}`, `create {customer, productId, quantity}` |
| POST | `/api/services/payments/{operation}` | `charge {orderId, amount}`, `refund {orderId, amount}` |
| GET | `/api/metrics/summary` | Per service: total calls, success rate, error rate, average response time |
| GET | `/api/metrics/logs?service=&status=&from=&to=&page=&size=` | Filtered logs (newest first) with pagination. `from`/`to` are ISO-8601 instants |
| POST | `/api/metrics/simulate-load` | Generates 50 random calls to populate the logs |

Example:

```bash
curl -X POST http://localhost:8080/api/services/inventory/check-stock \
  -H "content-type: application/json" -d '{"productId":"P-001"}'
curl http://localhost:8080/api/metrics/summary
```

## Run

Requires Java 17+ and Maven.

```bash
mvn test
mvn spring-boot:run
```

## Configuration

| Variable | Default | Description |
| --- | --- | --- |
| `PORT` | 8080 | HTTP port (Railway sets it automatically) |
| `CORS_ALLOWED_ORIGINS` | `*` | Comma separated list of allowed frontend origins |
| `PAYMENT_FAILURE_RATE` | 0.1 | Probability that a payment fails on purpose |
| `LOGS_CAPACITY` | 5000 | Maximum number of logs kept in memory |

## Deploy (Railway)

The repository includes a `Dockerfile` (multi-stage Maven build). In Railway: New Project, Deploy from GitHub repo, select this repository, then generate a public domain in Settings > Networking. Optionally set `CORS_ALLOWED_ORIGINS` to the Vercel URL of the frontend.
