import { apiFetch, urls } from '$lib/api';
import type { ProductShow } from '$lib/types';
import type { PageLoad } from './$types';

export const load: PageLoad = async ({ params, fetch }) => {
	const response = await apiFetch(urls.products.show(params.id), { fetch });

	if (!response.ok) {
		return { product: null };
	}

	const product: ProductShow = await response.json();
	return { product };
};
