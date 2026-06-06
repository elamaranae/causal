import { test, expect } from '../helpers/fixtures';
import { findInStockSkuId, findInStockSkuIds } from '../helpers/products';

test.describe('Cart API', () => {
  test('empty cart initially', async ({ authedClient }) => {
    const res = await authedClient.get('/cart/me');
    expect(res.status()).toBe(200);
    const cart = await res.json();
    expect(cart).toHaveProperty('items');
    expect(cart.items).toHaveLength(0);
  });

  test('add item to cart', async ({ authedClient }) => {
    const skuId = await findInStockSkuId(authedClient);

    const addRes = await authedClient.post('/cart/me/items', { skuId, quantity: 1 });
    expect(addRes.status()).toBe(200);

    const cartRes = await authedClient.get('/cart/me');
    const cart = await cartRes.json();
    expect(cart.items).toHaveLength(1);
    expect(cart.items[0].skuId).toBe(skuId);
    expect(cart.items[0].quantity).toBe(1);
  });

  test('update item quantity', async ({ authedClient }) => {
    const skuId = await findInStockSkuId(authedClient);
    await authedClient.post('/cart/me/items', { skuId, quantity: 1 });

    const cartRes = await authedClient.get('/cart/me');
    const cart = await cartRes.json();
    const itemId = cart.items[0].id;

    const updateRes = await authedClient.patch(`/cart/me/items/${itemId}`, { quantity: 3 });
    expect(updateRes.status()).toBe(200);

    const updatedCartRes = await authedClient.get('/cart/me');
    const updatedCart = await updatedCartRes.json();
    expect(updatedCart.items[0].quantity).toBe(3);
  });

  test('remove item from cart', async ({ authedClient }) => {
    const skuId = await findInStockSkuId(authedClient);
    await authedClient.post('/cart/me/items', { skuId, quantity: 1 });

    const cartRes = await authedClient.get('/cart/me');
    const cart = await cartRes.json();
    const itemId = cart.items[0].id;

    const deleteRes = await authedClient.delete(`/cart/me/items/${itemId}`);
    expect(deleteRes.status()).toBe(200);

    const emptyCartRes = await authedClient.get('/cart/me');
    const emptyCart = await emptyCartRes.json();
    expect(emptyCart.items).toHaveLength(0);
  });

  test('add multiple items', async ({ authedClient }) => {
    const skuIds = await findInStockSkuIds(authedClient, 2);

    await authedClient.post('/cart/me/items', { skuId: skuIds[0], quantity: 1 });
    await authedClient.post('/cart/me/items', { skuId: skuIds[1], quantity: 2 });

    const cartRes = await authedClient.get('/cart/me');
    const cart = await cartRes.json();
    expect(cart.items).toHaveLength(2);
  });
});
