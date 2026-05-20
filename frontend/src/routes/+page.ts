import { apiFetch, urls } from '$lib/api';
import type { ProductListing, Page } from '$lib/types';
import type { PageLoad } from './$types';

export const load: PageLoad = async ({ url, fetch }) => {
	const categoryId = url.searchParams.get('category');
	const page = Number(url.searchParams.get('page')) || 1;

	if (categoryId) {
		const res = await apiFetch(urls.products.filter(Number(categoryId), page), { fetch });
		if (res.ok) {
			const data: Page<ProductListing> = await res.json();
			return {
				products: data.content,
				pagination: data,
				categoryId: Number(categoryId),
				currentPage: page
			};
		}
		return {
			products: [] as ProductListing[],
			pagination: null,
			categoryId: Number(categoryId),
			currentPage: page
		};
	}

	const res = await apiFetch(urls.products.trending, { fetch });
	const products: ProductListing[] = res.ok ? await res.json() : [];
	return { products, pagination: null, categoryId: null, currentPage: 1 };
};
