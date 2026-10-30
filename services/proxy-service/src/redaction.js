const SENSITIVE_KEY = /pass(word)?|token|secret|authorization|api[-_]?key|card|cvv|ssn/i;
const SENSITIVE_TEXT = /\b(password|passwd|token|secret|api[-_]?key)\s*[=:]\s*\S+/gi;
const MASK = '[REDACTED]';

/** Masks sensitive values in free text and returns the cleaned text plus how many were masked. */
export function redactText(text) {
  let count = 0;
  const clean = text.replace(SENSITIVE_TEXT, (_match, key) => {
    count += 1;
    return `${key}=${MASK}`;
  });
  return { value: clean, count };
}

/** Recursively masks sensitive keys in metadata. */
export function redactMetadata(value, depth = 0) {
  if (depth > 5 || value === null || typeof value !== 'object') {
    return { value, count: 0 };
  }
  let count = 0;
  const result = Array.isArray(value) ? [] : {};
  for (const [key, inner] of Object.entries(value)) {
    if (SENSITIVE_KEY.test(key)) {
      result[key] = MASK;
      count += 1;
    } else {
      const nested = redactMetadata(inner, depth + 1);
      result[key] = nested.value;
      count += nested.count;
    }
  }
  return { value: result, count };
}
