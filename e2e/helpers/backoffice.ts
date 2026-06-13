import { request as playwrightRequest } from '@playwright/test';
import { ApiClient } from './api';

const ADMIN_EMAIL = 'admin@causal.dev';
const ADMIN_PASSWORD = 'CausalAdmin123!';

export interface CreatedProduct {
  productId: number;
  skuId: number;
  categoryId: number;
  categoryName: string;
}

export async function createAdminClient(): Promise<{
  client: ApiClient;
  dispose: () => Promise<void>;
}> {
  const ctx = await playwrightRequest.newContext();
  const client = new ApiClient(ctx);
  let lastError = '';
  for (let attempt = 0; attempt < 3; attempt++) {
    const res = await client.login(ADMIN_EMAIL, ADMIN_PASSWORD);
    if (res.ok()) return { client, dispose: () => ctx.dispose() };
    lastError = `${res.status()} ${await res.text()}`;
    await new Promise((r) => setTimeout(r, 500));
  }
  await ctx.dispose();
  throw new Error(`Failed to login admin after retries: ${lastError}`);
}

export async function createTestProduct(
  adminClient: ApiClient,
  options?: { stock?: number; price?: number; categoryName?: string },
): Promise<CreatedProduct> {
  const stock = options?.stock ?? 100;
  const price = options?.price ?? 29.99;
  const categoryName = options?.categoryName ?? `Test Category ${Date.now()}`;
  const suffix = Math.random().toString(36).slice(2, 8);

  const res = await adminClient.post('/products/backoffice/products', {
    name: `Test Product ${suffix}`,
    description: 'E2E test product',
    categoryName,
    primaryThumbnailUrl: 'https://placehold.co/400',
    primaryVariantKey: 'color',
    skus: [
      {
        variantAttributes: { color: 'Red' },
        price,
        currency: 'USD',
        stock,
      },
    ],
  });

  if (!res.ok()) {
    const body = await res.text();
    throw new Error(`Failed to create test product: ${res.status()} ${body}`);
  }

  const product = await res.json();
  return {
    productId: product.id,
    skuId: product.skus[0].id,
    categoryId: product.categoryId,
    categoryName,
  };
}

export async function createTestProducts(
  adminClient: ApiClient,
  count: number,
  options?: { stock?: number; categoryName?: string },
): Promise<CreatedProduct[]> {
  const categoryName = options?.categoryName ?? `Test Category ${Date.now()}`;
  const results: CreatedProduct[] = [];
  for (let i = 0; i < count; i++) {
    results.push(await createTestProduct(adminClient, { ...options, categoryName }));
  }
  return results;
}
