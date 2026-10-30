/** Small TTL cache for read queries. */
export class TtlCache {
  #items = new Map();

  constructor({ ttlMs, now = Date.now }) {
    this.ttlMs = ttlMs;
    this.now = now;
  }

  get(key) {
    const item = this.#items.get(key);
    if (!item) return undefined;
    if (this.now() >= item.expiresAt) {
      this.#items.delete(key);
      return undefined;
    }
    return item.value;
  }

  set(key, value) {
    this.#items.set(key, { value, expiresAt: this.now() + this.ttlMs });
  }

  clear() {
    this.#items.clear();
  }
}
