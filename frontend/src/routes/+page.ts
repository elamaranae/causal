import { ENDPOINTS, apiFetch } from '$lib/api/config';
import type { ProductListing } from '$lib/types';
import type { PageLoad } from './$types';

export const load: PageLoad = async ({ fetch }) => {
	const response = await apiFetch(ENDPOINTS.PRODUCTS.TRENDING, { fetch });

	if (!response.ok) {
		return {
			products: [] as ProductListing[]
		};
	}

	const products: ProductListing[] = await response.json();

	return {
		products
	};
};
