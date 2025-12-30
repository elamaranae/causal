import { ENDPOINTS } from '$lib/api/config';
import type { Product, CartItem } from './types';

class CartStore {
	#items = $state<CartItem[]>([]);

	get items() {
		return this.#items;
	}

	get totalItems() {
		return this.#items.reduce((sum, item) => sum + item.quantity, 0);
	}

	get totalPrice() {
		return this.#items.reduce((sum, item) => sum + item.product.price * item.quantity, 0);
	}

	getItemQuantity(productId: number) {
		return this.#items.find((item) => item.product.id === productId)?.quantity || 0;
	}

	async addItem(product: Product) {
		const existingItem = this.#items.find((item) => item.product.id === product.id);

		if (existingItem) {
			existingItem.quantity += 1;
		} else {
			this.#items.push({ product, quantity: 1 });
		}

		// Optional: Sync with API
		/*
		await fetch(ENDPOINTS.CART.ADD, {
			method: 'POST',
			body: JSON.stringify({ productId: product.id, quantity: 1 })
		});
		*/
	}

	async removeItem(productId: number) {
		this.#items = this.#items.filter((item) => item.product.id !== productId);
	}

	async updateQuantity(productId: number, quantity: number) {
		const item = this.#items.find((item) => item.product.id === productId);
		if (item) {
			item.quantity = quantity;
			if (item.quantity <= 0) {
				await this.removeItem(productId);
			}
		}
	}
}

export const cart = new CartStore();
