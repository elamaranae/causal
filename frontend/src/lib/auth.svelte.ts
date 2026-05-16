import { browser } from '$app/environment';
import type { User } from './types';
import { ENDPOINTS, apiFetch } from './api/config';

class AuthState {
	user = $state<User | null>(browser ? JSON.parse(localStorage.getItem('user') || 'null') : null);
	isAuthenticated = $state<boolean>(browser ? localStorage.getItem('isAuthenticated') === 'true' : false);

	async fetchUser() {
		try {
			const response = await apiFetch(ENDPOINTS.AUTH.ME);
			if (response.ok) {
				const userData = await response.json();
				this.user = userData;
				this.isAuthenticated = true;
				if (browser) {
					localStorage.setItem('user', JSON.stringify(userData));
					localStorage.setItem('isAuthenticated', 'true');
				}
			} else {
				this.clearLocal();
			}
		} catch (error) {
			console.error('Failed to fetch user:', error);
			this.clearLocal();
		}
	}

	async onLoginSuccess() {
		this.isAuthenticated = true;
		if (browser) {
			localStorage.setItem('isAuthenticated', 'true');
		}
		await this.fetchUser();
	}

	async logout() {
		try {
			await apiFetch(ENDPOINTS.AUTH.LOGOUT, { method: 'POST' });
		} catch (error) {
			console.error('Failed to call logout endpoint:', error);
		} finally {
			this.clearLocal();
		}
	}

	private clearLocal() {
		this.user = null;
		this.isAuthenticated = false;
		if (browser) {
			localStorage.removeItem('user');
			localStorage.removeItem('isAuthenticated');
		}
	}
}

export const auth = new AuthState();
