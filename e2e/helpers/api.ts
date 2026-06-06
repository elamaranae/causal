import type { APIRequestContext } from '@playwright/test';

const BASE_URL = process.env.BASE_URL || 'http://causal-gateway';

/**
 * API client that manages cookies (auth + CSRF) across requests.
 * Works with Playwright's APIRequestContext for cookie persistence.
 */
export class ApiClient {
  constructor(private request: APIRequestContext) {}

  private csrfToken: string | null = null;

  private extractCsrf(headers: Record<string, string>) {
    const setCookie = headers['set-cookie'];
    if (!setCookie) return;
    const match = setCookie.match(/csrf_token=([^;]+)/);
    if (match) this.csrfToken = match[1];
  }

  async get(path: string) {
    const res = await this.request.get(`${BASE_URL}${path}`, {
      headers: { Accept: 'application/json' },
    });
    this.extractCsrf(res.headers());
    return res;
  }

  async post(path: string, data?: unknown) {
    const headers: Record<string, string> = { Accept: 'application/json' };
    if (this.csrfToken) headers['X-CSRF-Token'] = this.csrfToken;
    if (data !== undefined) headers['Content-Type'] = 'application/json';

    const res = await this.request.post(`${BASE_URL}${path}`, {
      headers,
      data,
    });
    this.extractCsrf(res.headers());
    return res;
  }

  async patch(path: string, data?: unknown) {
    const headers: Record<string, string> = { Accept: 'application/json' };
    if (this.csrfToken) headers['X-CSRF-Token'] = this.csrfToken;
    if (data !== undefined) headers['Content-Type'] = 'application/json';

    const res = await this.request.patch(`${BASE_URL}${path}`, {
      headers,
      data,
    });
    this.extractCsrf(res.headers());
    return res;
  }

  async delete(path: string) {
    const headers: Record<string, string> = { Accept: 'application/json' };
    if (this.csrfToken) headers['X-CSRF-Token'] = this.csrfToken;

    const res = await this.request.delete(`${BASE_URL}${path}`, { headers });
    this.extractCsrf(res.headers());
    return res;
  }

  async register(email: string, password: string) {
    return this.post('/auth/register', { email, password });
  }

  async login(email: string, password: string) {
    return this.post('/auth/login', { email, password });
  }
}
