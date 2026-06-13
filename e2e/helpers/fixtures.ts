import { test as base, request as playwrightRequest } from '@playwright/test';
import { ApiClient } from './api';

export const TEST_PASSWORD = 'TestPass123!';
const ADMIN_EMAIL = 'admin@causal.dev';
const ADMIN_PASSWORD = 'CausalAdmin123!';

function uniqueEmail(): string {
  return `e2e-api-${Date.now()}-${Math.random().toString(36).slice(2, 8)}@test.dev`;
}

type Fixtures = {
  apiClient: ApiClient;
  authedClient: ApiClient;
  adminClient: ApiClient;
  testEmail: string;
};

export const test = base.extend<Fixtures>({
  apiClient: async ({}, use) => {
    const ctx = await playwrightRequest.newContext();
    await use(new ApiClient(ctx));
    await ctx.dispose();
  },

  testEmail: async ({}, use) => {
    await use(uniqueEmail());
  },

  authedClient: async ({ testEmail }, use) => {
    const ctx = await playwrightRequest.newContext();
    const client = new ApiClient(ctx);
    const res = await client.register(testEmail, TEST_PASSWORD);
    if (!res.ok()) {
      const body = await res.text();
      await ctx.dispose();
      throw new Error(`Failed to register test user: ${res.status()} ${body}`);
    }
    await use(client);
    await ctx.dispose();
  },

  adminClient: async ({}, use) => {
    const ctx = await playwrightRequest.newContext();
    const client = new ApiClient(ctx);
    let lastError = '';
    for (let attempt = 0; attempt < 3; attempt++) {
      const res = await client.login(ADMIN_EMAIL, ADMIN_PASSWORD);
      if (res.ok()) {
        await use(client);
        await ctx.dispose();
        return;
      }
      lastError = `${res.status()} ${await res.text()}`;
      await new Promise((r) => setTimeout(r, 500));
    }
    await ctx.dispose();
    throw new Error(`Failed to login admin after retries: ${lastError}`);
  },
});

export { expect } from '@playwright/test';
