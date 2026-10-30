/** Fixed-window rate limiter keyed by client name. */
export class RateLimiter {
  #windows = new Map();

  constructor({ limit, windowMs, now = Date.now }) {
    this.limit = limit;
    this.windowMs = windowMs;
    this.now = now;
  }

  /** Returns true when the request is allowed. */
  consume(key) {
    const current = this.now();
    const window = this.#windows.get(key);
    if (!window || current - window.start >= this.windowMs) {
      this.#windows.set(key, { start: current, count: 1 });
      return true;
    }
    if (window.count >= this.limit) return false;
    window.count += 1;
    return true;
  }
}
