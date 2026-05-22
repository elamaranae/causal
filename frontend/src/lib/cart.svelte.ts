import type { ApiCart, ApiCartItem, CartItem, SkuDetail } from './types';
import { apiFetch, urls } from './api';

class CartStore {
  #items = $state<CartItem[]>([]);
  #loading = $state(false);

  get items() {
    return this.#items;
  }

  get loading() {
    return this.#loading;
  }

  get totalItems() {
    return this.#items.reduce((sum, item) => sum + item.quantity, 0);
  }

  get totalPrice() {
    return this.#items.reduce((sum, item) => sum + item.sku.price.priceAmount * item.quantity, 0);
  }

  async load() {
    if (this.#items.length === 0) this.#loading = true;
    try {
      const cartRes = await apiFetch(urls.cart.me);
      if (!cartRes.ok) {
        this.#items = [];
        return;
      }
      const cart: ApiCart = await cartRes.json();
      if (cart.items.length === 0) {
        this.#items = [];
        return;
      }

      const skuIds = cart.items.map((i) => i.skuId);
      const skuRes = await apiFetch(urls.products.skusBulk, {
        method: 'POST',
        body: JSON.stringify({ ids: skuIds })
      });
      const skus: SkuDetail[] = skuRes.ok ? await skuRes.json() : [];
      const skuMap = new Map(skus.map((s) => [s.id, s]));

      this.#items = cart.items
        .filter((item) => skuMap.has(item.skuId))
        .map((item) => ({
          cartItemId: item.id,
          skuId: item.skuId,
          quantity: item.quantity,
          sku: skuMap.get(item.skuId)!
        }));
    } finally {
      this.#loading = false;
    }
  }

  async addItem(skuId: number, quantity = 1) {
    const res = await apiFetch(urls.cart.addItem, {
      method: 'POST',
      body: JSON.stringify({ skuId, quantity })
    });
    if (res.ok) await this.load();
  }

  async updateQuantity(cartItemId: number, quantity: number) {
    if (quantity <= 0) {
      await this.removeItem(cartItemId);
      return;
    }
    const res = await apiFetch(urls.cart.updateItem(cartItemId), {
      method: 'PATCH',
      body: JSON.stringify({ quantity })
    });
    if (res.ok) await this.load();
  }

  async removeItem(cartItemId: number) {
    const res = await apiFetch(urls.cart.removeItem(cartItemId), {
      method: 'DELETE'
    });
    if (res.ok) await this.load();
  }

  clear() {
    this.#items = [];
  }
}

export const cart = new CartStore();
