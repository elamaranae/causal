import { test, expect } from '../helpers/fixtures';

test.describe('Products API', () => {
  test('get trending products', async ({ authedClient }) => {
    const res = await authedClient.get('/products/trending');
    expect(res.status()).toBe(200);
    const products = await res.json();
    expect(Array.isArray(products)).toBe(true);
    expect(products.length).toBeGreaterThan(0);
    expect(products[0]).toHaveProperty('id');
    expect(products[0]).toHaveProperty('name');
    expect(products[0]).toHaveProperty('defaultSku');
  });

  test('get categories', async ({ authedClient }) => {
    const res = await authedClient.get('/products/categories');
    expect(res.status()).toBe(200);
    const categories = await res.json();
    expect(Array.isArray(categories)).toBe(true);
    expect(categories.length).toBeGreaterThan(0);
    expect(categories[0]).toHaveProperty('id');
    expect(categories[0]).toHaveProperty('name');
  });

  test('filter products by category', async ({ authedClient }) => {
    const catRes = await authedClient.get('/products/categories');
    const categories = await catRes.json() as Array<{ id: number; parentId: number | null }>;
    // Use a leaf category (has parentId) to avoid issues with parent categories
    const leafCategory = categories.find((c) => c.parentId !== null);
    expect(leafCategory).toBeDefined();
    const categoryId = leafCategory!.id;

    const res = await authedClient.get(`/products/filter?categoryId=${categoryId}&page=0&size=5`);
    expect(res.status()).toBe(200);
    const page = await res.json();
    expect(page).toHaveProperty('content');
    expect(page).toHaveProperty('totalElements');
  });

  test('get single product by ID', async ({ authedClient }) => {
    const trendingRes = await authedClient.get('/products/trending');
    const products = await trendingRes.json();
    const productId = products[0].id;

    const res = await authedClient.get(`/products/${productId}`);
    expect(res.status()).toBe(200);
    const product = await res.json();
    expect(product).toHaveProperty('id', productId);
    expect(product).toHaveProperty('name');
    expect(product).toHaveProperty('skus');
    expect(product.skus.length).toBeGreaterThan(0);
  });

  test('bulk fetch SKUs', async ({ authedClient }) => {
    const trendingRes = await authedClient.get('/products/trending');
    const products = await trendingRes.json();
    const skuIds = products.slice(0, 3).map((p: { defaultSku: { id: number } }) => p.defaultSku.id);

    const res = await authedClient.post('/products/skus/bulk', { ids: skuIds });
    expect(res.status()).toBe(200);
    const skus = await res.json();
    expect(Array.isArray(skus)).toBe(true);
    expect(skus.length).toBe(skuIds.length);
    expect(skus[0]).toHaveProperty('product');
  });
});
