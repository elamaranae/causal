import { test as base, request as playwrightRequest } from '@playwright/test';
import { ApiClient } from './api';

export const TEST_PASSWORD = 'TestPass123!';

function uniqueEmail(): string {
  return `e2e-api-${Date.now()}-${Math.random().toString(36).slice(2, 8)}@test.dev`;
}

type Fixtures = {
  apiClient: ApiClient;
  authedClient: ApiClient;
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
});

export { expect } from '@playwright/test';
