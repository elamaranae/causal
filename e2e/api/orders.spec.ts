import { test, expect } from '../helpers/fixtures';
import { findInStockSkuId } from '../helpers/products';
import type { ApiClient } from '../helpers/api';

async function setupForCheckout(client: ApiClient) {
  const skuId = await findInStockSkuId(client);
  await client.post('/cart/me/items', { skuId, quantity: 1 });

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

test.describe('Orders API', () => {
  test('full checkout flow', async ({ authedClient }) => {
    await setupForCheckout(authedClient);

    const checkoutRes = await authedClient.post('/orders/checkout');
    expect(checkoutRes.status()).toBe(200);
    const order = await checkoutRes.json();
    expect(order).toHaveProperty('id');
    expect(order).toHaveProperty('status');
    expect(order).toHaveProperty('items');
    expect(order.items.length).toBeGreaterThan(0);
  });

  test('get order by ID', async ({ authedClient }) => {
    await setupForCheckout(authedClient);
    const checkoutRes = await authedClient.post('/orders/checkout');
    const order = await checkoutRes.json();

    const showRes = await authedClient.get(`/orders/${order.id}`);
    expect(showRes.status()).toBe(200);
    const fetched = await showRes.json();
    expect(fetched.id).toBe(order.id);
  });

  test('list orders with pagination', async ({ authedClient }) => {
    await setupForCheckout(authedClient);
    await authedClient.post('/orders/checkout');

    const res = await authedClient.get('/orders?page=0&size=10');
    expect(res.status()).toBe(200);
    const data = await res.json();
    expect(data).toHaveProperty('orders');
    expect(data.orders.length).toBeGreaterThan(0);
  });
});
