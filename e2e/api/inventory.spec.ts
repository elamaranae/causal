import { test, expect, TEST_PASSWORD } from '../helpers/fixtures';
import { createTestProduct } from '../helpers/backoffice';
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
  test('cart rejects quantity exceeding total stock', async ({ authedClient, adminClient }) => {
    const { skuId } = await createTestProduct(adminClient, { stock: 5 });

    const res = await authedClient.post('/cart/me/items', {
      skuId,
      quantity: 6,
    });
    expect(res.status()).toBe(422);
    const body = await res.text();
    expect(body).toContain('Insufficient stock');
  });

  test('cart rejects adding out-of-stock SKU', async ({ authedClient, adminClient }) => {
    const { skuId } = await createTestProduct(adminClient, { stock: 0 });

    const res = await authedClient.post('/cart/me/items', {
      skuId,
      quantity: 1,
    });
    expect(res.status()).toBe(422);
  });

  test('checkout succeeds and creates order with RESERVED status', async ({
    authedClient,
    adminClient,
  }) => {
    const { skuId } = await createTestProduct(adminClient, { stock: 10 });
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
    adminClient,
  }) => {
    const { skuId } = await createTestProduct(adminClient, { stock: 1 });

    await setupForCheckout(authedClient, skuId, 1);
    const { client: client2, dispose } = await createAuthedClient();
    await setupForCheckout(client2, skuId, 1);

    const [res1, res2] = await Promise.all([
      authedClient.post('/orders/checkout'),
      client2.post('/orders/checkout'),
    ]);

    const statuses = [res1.status(), res2.status()].sort();
    expect(statuses[0] === 200 || statuses[0] === 409).toBe(true);
    expect(statuses[1]).toBe(409);
    const successes = [res1.status(), res2.status()].filter((s) => s === 200).length;
    expect(successes).toBeLessThanOrEqual(1);

    await dispose();
  });
});
