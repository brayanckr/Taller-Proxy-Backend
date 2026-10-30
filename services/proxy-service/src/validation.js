import { ProxyError } from './errors.js';
import { redactMetadata, redactText } from './redaction.js';

export const LEVELS = ['DEBUG', 'INFO', 'WARN', 'ERROR', 'FATAL'];
const MAX_MESSAGE_LENGTH = 1000;

/**
 * Validates an incoming log and returns a sanitized copy.
 * Producers cannot spoof another service: the service name is taken from the API key.
 */
export function buildLogEntry(client, input) {
  if (input === null || typeof input !== 'object' || Array.isArray(input)) {
    throw new ProxyError(400, 'invalid_log', 'Body must be a JSON object');
  }
  const level = String(input.level ?? '').toUpperCase();
  if (!LEVELS.includes(level)) {
    throw new ProxyError(400, 'invalid_log', `level must be one of ${LEVELS.join(', ')}`);
  }
  if (typeof input.message !== 'string' || !input.message.trim()) {
    throw new ProxyError(400, 'invalid_log', 'message is required');
  }
  if (input.message.length > MAX_MESSAGE_LENGTH) {
    throw new ProxyError(400, 'invalid_log', `message exceeds ${MAX_MESSAGE_LENGTH} characters`);
  }
  if (input.service && input.service !== client.name) {
    throw new ProxyError(403, 'service_mismatch', `API key belongs to "${client.name}", not "${input.service}"`);
  }
  let timestamp;
  if (input.timestamp !== undefined) {
    if (Number.isNaN(Date.parse(input.timestamp))) {
      throw new ProxyError(400, 'invalid_log', 'timestamp must be a valid date');
    }
    timestamp = new Date(input.timestamp).toISOString();
  }

  const message = redactText(input.message.trim());
  const metadata = redactMetadata(input.metadata ?? {});
  return {
    entry: {
      service: client.name,
      level,
      message: message.value,
      metadata: metadata.value,
      ...(timestamp && { timestamp }),
    },
    redactions: message.count + metadata.count,
  };
}
