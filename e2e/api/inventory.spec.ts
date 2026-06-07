import { test, expect, TEST_PASSWORD } from '../helpers/fixtures';
import { findInStockSkuWithStock } from '../helpers/products';
import { ApiClient } from '../helpers/api';
import { request as playwrightRequest } from '@playwright/test';

async function createAuthedClient(): Promise<{ client: ApiClient; dispose: () => Promise<void> }> {
  const ctx = await playwrightRequest.newContext();
  const client = new ApiClient(ctx);
  const email = `e2e-inv-${Date.now()}-${Math.random().toString(36).slice(2, 8)}@test.dev`;
  await client.register(email, TEST_PASSWORD);
  return { client, dispose: () => ctx.dispose() };
}

async function setupForCheckout(client: ApiClient, skuId: number, quantity: number) {
  await client.post('/cart/me/items', { skuId, quantity });
  await client.post('/profiles/me', {
    firstName: 'Test',
    lastName: 'Buyer',
    currency: 'USD',
  });
  await client.post('/profiles/me/addresses', {
    label: 'Shipping',
    line1: '100 Commerce Blvd',
    city: 'Shoptown',
    state: 'CA',
    country: 'US',
    pincode: '90210',
    phoneNumber: '555-0123',
  });
}

test.describe('Inventory locking', () => {
  test('cart rejects quantity exceeding total stock', async ({ authedClient }) => {
    const { skuId, stockQuantity } = await findInStockSkuWithStock(authedClient);

    const res = await authedClient.post('/cart/me/items', {
      skuId,
      quantity: stockQuantity + 1,
    });
    expect(res.status()).toBe(422);
    const body = await res.text();
    expect(body).toContain('Insufficient stock');
  });

  test('cart rejects adding out-of-stock SKU', async ({ authedClient }) => {
    const trendingRes = await authedClient.get('/products/trending');
    const products = await trendingRes.json();

    let outOfStockSkuId: number | null = null;
    for (const product of products) {
      const detailRes = await authedClient.get(`/products/${product.id}`);
      const detail = await detailRes.json();
      const sku = detail.skus.find((s: { stockQuantity: number }) => s.stockQuantity === 0);
      if (sku) {
        outOfStockSkuId = sku.id;
        break;
      }
    }

    if (outOfStockSkuId === null) {
      test.skip();
      return;
    }

    const res = await authedClient.post('/cart/me/items', {
      skuId: outOfStockSkuId,
      quantity: 1,
    });
    expect(res.status()).toBe(422);
  });

  test('checkout succeeds and creates order with RESERVED status', async ({ authedClient }) => {
    const { skuId } = await findInStockSkuWithStock(authedClient);
    await setupForCheckout(authedClient, skuId, 1);

    const res = await authedClient.post('/orders/checkout');
    expect(res.status()).toBe(200);
    const order = await res.json();
    expect(order.status).toBe('RESERVED');
    expect(order.items).toHaveLength(1);
    expect(order.items[0].skuId).toBe(skuId);
    expect(order.items[0].quantity).toBe(1);
  });

  test('concurrent checkouts for limited stock — at most one succeeds', async ({
    authedClient,
  }) => {
    // Find a SKU with stock = 1 so exactly one checkout can reserve it
    const { skuId } = await findInStockSkuWithStock(authedClient, 1);

    // Use a large quantity equal to the total stock to ensure contention
    // Both users request the max — at most one can win the reservation
    const { skuId: skuIdCheck, stockQuantity } = await findInStockSkuWithStock(authedClient, 1);

    await setupForCheckout(authedClient, skuIdCheck, stockQuantity);
    const { client: client2, dispose } = await createAuthedClient();
    await setupForCheckout(client2, skuIdCheck, stockQuantity);

    const [res1, res2] = await Promise.all([
      authedClient.post('/orders/checkout'),
      client2.post('/orders/checkout'),
    ]);

    const statuses = [res1.status(), res2.status()].sort();
    // Either one succeeds and one conflicts, or both conflict (if prior reservations exist)
    expect(statuses[0] === 200 || statuses[0] === 409).toBe(true);
    expect(statuses[1]).toBe(409);
    // At most one should succeed
    const successes = [res1.status(), res2.status()].filter((s) => s === 200).length;
    expect(successes).toBeLessThanOrEqual(1);

    await dispose();
  });
});
