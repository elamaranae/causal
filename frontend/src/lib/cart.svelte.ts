import { browser } from '$app/environment';
import type { ProductListing, CartItem } from './types';

class CartStore {
	#items = $state<CartItem[]>(browser ? JSON.parse(localStorage.getItem('cart') || '[]') : []);

	get items() {
		return this.#items;
	}

	get totalItems() {
		return this.#items.reduce((sum, item) => sum + item.quantity, 0);
	}

	getItemQuantity(productId: number) {
		return this.#items.find((item) => item.product.id === productId)?.quantity || 0;
	}

	addItem(product: ProductListing) {
		const existing = this.#items.find((item) => item.product.id === product.id);
		if (existing) {
			existing.quantity += 1;
		} else {
			this.#items.push({ product, quantity: 1 });
		}
		this.#persist();
	}

	removeItem(productId: number) {
		this.#items = this.#items.filter((item) => item.product.id !== productId);
		this.#persist();
	}

	updateQuantity(productId: number, quantity: number) {
		if (quantity <= 0) {
			this.removeItem(productId);
			return;
		}
		const item = this.#items.find((item) => item.product.id === productId);
		if (item) {
			item.quantity = quantity;
			this.#persist();
		}
	}

	#persist() {
		if (browser) localStorage.setItem('cart', JSON.stringify(this.#items));
	}
}

export const cart = new CartStore();
