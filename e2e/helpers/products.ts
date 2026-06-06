import type { ApiClient } from './api';

interface TrendingProduct {
  id: number;
  inStock: boolean;
  defaultSku: { id: number };
}

interface ProductDetail {
  skus: Array<{ id: number; stockQuantity: number }>;
}

export async function findInStockSkuId(client: ApiClient): Promise<number> {
  const res = await client.get('/products/trending');
  const products = (await res.json()) as TrendingProduct[];

  for (const product of products) {
    if (!product.inStock) continue;
    const detailRes = await client.get(`/products/${product.id}`);
    const detail = (await detailRes.json()) as ProductDetail;
    const sku = detail.skus.find((s) => s.stockQuantity > 0);
    if (sku) return sku.id;
  }
  throw new Error('No in-stock SKU found in trending products');
}

export async function findInStockSkuIds(client: ApiClient, count: number): Promise<number[]> {
  const res = await client.get('/products/trending');
  const products = (await res.json()) as TrendingProduct[];
  const ids: number[] = [];

  for (const product of products) {
    if (!product.inStock || ids.length >= count) continue;
    const detailRes = await client.get(`/products/${product.id}`);
    const detail = (await detailRes.json()) as ProductDetail;
    const sku = detail.skus.find((s) => s.stockQuantity > 0);
    if (sku) ids.push(sku.id);
  }

  if (ids.length < count) throw new Error(`Need ${count} in-stock SKUs, found ${ids.length}`);
  return ids;
}
