import { apiFetch, urls } from '$lib/api';
import type { ProductCategory } from '$lib/types';
import type { LayoutLoad } from './$types';

export const ssr = false;

export const load: LayoutLoad = async ({ fetch }) => {
  const res = await apiFetch(urls.products.categories, { fetch });
  const categories: ProductCategory[] = res.ok ? await res.json() : [];
  return { categories };
};
