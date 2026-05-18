import type { ProductListing, CartItem } from './types';

class CartStore {
	#items = $state<CartItem[]>([]);

	get items() {
		return this.#items;
	}

	get totalItems() {
		return this.#items.reduce((sum, item) => sum + item.quantity, 0);
	}

	getItemQuantity(productId: number) {
		return this.#items.find((item) => item.product.id === productId)?.quantity || 0;
	}

	async addItem(product: ProductListing) {
		const existingItem = this.#items.find((item) => item.product.id === product.id);

		if (existingItem) {
			existingItem.quantity += 1;
		} else {
			this.#items.push({ product, quantity: 1 });
		}
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
