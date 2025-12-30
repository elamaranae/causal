import { browser } from '$app/environment';
import type { User } from './types';
import { ENDPOINTS, apiFetch } from './api/config';

class AuthState {
	accessToken = $state<string | null>(browser ? localStorage.getItem('accessToken') : null);
	refreshToken = $state<string | null>(browser ? localStorage.getItem('refreshToken') : null);
	user = $state<User | null>(browser ? JSON.parse(localStorage.getItem('user') || 'null') : null);

	async fetchUser() {
		try {
			const response = await apiFetch(ENDPOINTS.AUTH.ME);
			if (response.ok) {
				const userData = await response.json();
				this.user = userData;
				if (browser) {
					localStorage.setItem('user', JSON.stringify(userData));
				}
			}
		} catch (error) {
			console.error('Failed to fetch user:', error);
		}
	}

	async setAuth(accessToken: string, refreshToken: string, user?: User) {
		this.accessToken = accessToken;
		this.refreshToken = refreshToken;
		
		if (browser) {
			localStorage.setItem('accessToken', accessToken);
			localStorage.setItem('refreshToken', refreshToken);
		}

		if (user) {
			this.user = user;
			if (browser) {
				localStorage.setItem('user', JSON.stringify(user));
			}
		} else {
			await this.fetchUser();
		}
	}

	async refreshTokens(): Promise<boolean> {
		if (!this.refreshToken) return false;

		try {
			const response = await fetch(ENDPOINTS.AUTH.REFRESH, {
				method: 'POST',
				headers: {
					'Content-Type': 'application/json'
				},
				body: JSON.stringify({ refreshToken: this.refreshToken })
			});

			if (response.ok) {
				const data = await response.json();
				this.setAuth(data.accessToken, data.refreshToken || this.refreshToken, data.user || this.user);
				return true;
			}

			await this.logout();
			return false;
		} catch (error) {
			console.error('Token refresh failed:', error);
			await this.logout();
			return false;
		}
	}

	async logout() {
		try {
			await apiFetch(ENDPOINTS.AUTH.LOGOUT, { method: 'POST' });
		} catch (error) {
			console.error('Failed to call logout endpoint:', error);
		} finally {
			this.accessToken = null;
			this.refreshToken = null;
			this.user = null;

			if (browser) {
				localStorage.removeItem('accessToken');
				localStorage.removeItem('refreshToken');
				localStorage.removeItem('user');
			}
		}
	}

	get isAuthenticated() {
		return !!this.accessToken;
	}
}

export const auth = new AuthState();
