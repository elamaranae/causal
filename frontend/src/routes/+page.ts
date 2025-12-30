import { ENDPOINTS, apiFetch } from '$lib/api/config';
import type { Product } from '$lib/types';
import type { PageLoad } from './$types';

export const load: PageLoad = async ({ fetch }) => {
	const response = await apiFetch(ENDPOINTS.PRODUCTS.LIST, { fetch });
	
	if (!response.ok) {
		return {
			products: [] as Product[]
		};
	}

	const products: Product[] = await response.json();

	return {
		products
	};
};
