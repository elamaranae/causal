import { defineConfig } from '@playwright/test';

const BASE_URL = process.env.BASE_URL || 'http://causal-gateway';

export default defineConfig({
  timeout: 30_000,
  expect: { timeout: 10_000 },
  fullyParallel: true,
  retries: 0,
  reporter: 'list',

  projects: [
    {
      name: 'api',
      testDir: './api',
      use: { baseURL: BASE_URL },
    },
    {
      name: 'browser',
      testDir: './browser',
      use: {
        baseURL: BASE_URL,
        browserName: 'chromium',
        screenshot: 'only-on-failure',
      },
    },
  ],
});
