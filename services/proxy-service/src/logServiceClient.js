import { ProxyError } from './errors.js';

/** Real subject client: talks to the internal log-service over HTTP. */
export class HttpLogService {
  constructor({ baseUrl, internalToken, fetchImpl = fetch }) {
    this.baseUrl = baseUrl;
    this.internalToken = internalToken;
    this.fetch = fetchImpl;
  }

  async #request(path, options = {}) {
    let response;
    try {
      response = await this.fetch(`${this.baseUrl}${path}`, {
        ...options,
        headers: {
          'content-type': 'application/json',
          'x-internal-token': this.internalToken,
          ...options.headers,
        },
        signal: AbortSignal.timeout(3000),
      });
    } catch (cause) {
      throw new ProxyError(503, 'log_service_unavailable', 'The log service is not reachable', cause.message);
    }
    if (!response.ok) {
      const body = await response.json().catch(() => ({}));
      throw new ProxyError(response.status >= 500 ? 502 : response.status, 'log_service_error', body.message ?? 'Log service error');
    }
    return response.status === 204 ? null : response.json();
  }

  createLog(entry) {
    return this.#request('/logs', { method: 'POST', body: JSON.stringify(entry) });
  }

  listLogs(filters) {
    const query = new URLSearchParams(Object.entries(filters).filter(([, value]) => value)).toString();
    return this.#request(`/logs${query ? `?${query}` : ''}`);
  }

  getStats() {
    return this.#request('/logs/stats');
  }

  async isHealthy() {
    try {
      await this.#request('/health');
      return true;
    } catch {
      return false;
    }
  }
}
