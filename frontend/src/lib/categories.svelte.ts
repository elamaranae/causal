import { ENDPOINTS, apiFetch } from '$lib/api/config';
import type { ProductCategory } from './types';

class CategoryStore {
	#categories = $state<ProductCategory[]>([]);
	#loaded = $state(false);
	#selectedId = $state<number | null>(null);

	get categories() {
		return this.#categories;
	}

	get loaded() {
		return this.#loaded;
	}

	get selectedId() {
		return this.#selectedId;
	}

	get topLevel() {
		return this.#categories.filter((c) => !c.parentId);
	}

	childrenOf(parentId: number) {
		return this.#categories.filter((c) => c.parentId === parentId);
	}

	select(id: number | null) {
		this.#selectedId = id;
	}

	async load(fetchFn?: typeof fetch) {
		if (this.#loaded) return;
		const response = await apiFetch(ENDPOINTS.PRODUCTS.CATEGORIES, { fetch: fetchFn });
		if (response.ok) {
			this.#categories = await response.json();
		}
		this.#loaded = true;
	}
}

export const categories = new CategoryStore();
