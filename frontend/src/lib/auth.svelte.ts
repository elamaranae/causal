import { browser } from '$app/environment';
import type { User } from './types';
import { apiFetch, urls } from './api';

class AuthState {
  user = $state<User | null>(browser ? JSON.parse(localStorage.getItem('user') || 'null') : null);

  get isAuthenticated(): boolean {
    return this.user !== null;
  }

  async fetchUser() {
    try {
      const res = await apiFetch(urls.auth.me);
      if (res.ok) {
        this.user = await res.json();
        if (browser) localStorage.setItem('user', JSON.stringify(this.user));
      } else {
        this.clear();
      }
    } catch {
      this.clear();
    }
  }

  async onLoginSuccess() {
    await this.fetchUser();
  }

  async logout() {
    try {
      await apiFetch(urls.auth.logout, { method: 'POST' });
    } finally {
      this.clear();
    }
  }

  private clear() {
    this.user = null;
    if (browser) localStorage.removeItem('user');
  }
}

export const auth = new AuthState();
