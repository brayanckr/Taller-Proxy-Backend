/**
 * Simulates several business microservices that emit logs.
 * They never talk to the log-service directly: every log goes through the proxy.
 */
const proxyUrl = process.env.PROXY_URL || 'http://localhost:4000';
const intervalMs = Number(process.env.INTERVAL_MS) || 1500;

const producers = [
  {
    apiKey: 'producer-key-auth',
    name: 'auth-service',
    events: [
      ['INFO', 'User logged in successfully'],
      ['WARN', 'Invalid password attempt for user ana@example.com password=123456'],
      ['ERROR', 'Token validation failed'],
    ],
  },
  {
    apiKey: 'producer-key-payments',
    name: 'payments-service',
    events: [
      ['INFO', 'Payment approved'],
      ['ERROR', 'Payment gateway timeout'],
      ['FATAL', 'Connection to the bank lost'],
    ],
  },
  {
    apiKey: 'producer-key-orders',
    name: 'orders-service',
    events: [
      ['DEBUG', 'Order total recalculated'],
      ['INFO', 'Order created'],
      ['WARN', 'Stock is running low'],
    ],
  },
];

const pick = (list) => list[Math.floor(Math.random() * list.length)];

async function emit() {
  const producer = pick(producers);
  const [level, message] = pick(producer.events);
  try {
    const response = await fetch(`${proxyUrl}/api/logs`, {
      method: 'POST',
      headers: { 'content-type': 'application/json', 'x-api-key': producer.apiKey },
      body: JSON.stringify({ level, message, metadata: { requestId: Math.random().toString(36).slice(2, 10) } }),
    });
    console.log(`${producer.name} -> ${level} (${response.status})`);
  } catch (error) {
    console.error(`${producer.name} could not reach the proxy: ${error.message}`);
  }
}

console.log(`Simulator sending logs to ${proxyUrl} every ${intervalMs} ms`);
setInterval(emit, intervalMs);
