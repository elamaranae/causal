import type { ApiClient } from './api';

interface TrendingProduct {
  id: number;
  inStock: boolean;
  defaultSku: { id: number };
}

interface Sku {
  id: number;
  stockQuantity: number;
  variantAttributes: Record<string, string>;
}

interface ProductDetail {
  skus: Sku[];
}

export interface InStockProduct {
  productId: number;
  variantAttributes: Record<string, string>;
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

export async function findInStockSkuId(client: ApiClient): Promise<number> {
  return (await findInStockSkuIds(client, 1))[0];
}

/** Returns a trending product ID and the variant attributes of an in-stock SKU. */
export async function findInStockProduct(client: ApiClient): Promise<InStockProduct> {
  const res = await client.get('/products/trending');
  const products = (await res.json()) as TrendingProduct[];

  for (const product of products) {
    if (!product.inStock) continue;
    const detailRes = await client.get(`/products/${product.id}`);
    const detail = (await detailRes.json()) as ProductDetail;
    const sku = detail.skus.find((s) => s.stockQuantity > 0);
    if (sku) return { productId: product.id, variantAttributes: sku.variantAttributes };
  }

  throw new Error('No trending product with an in-stock SKU found');
}
