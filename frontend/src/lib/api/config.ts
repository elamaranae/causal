import { auth } from '$lib/auth.svelte';

export const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8090';

const ROUTES = {
	AUTH: {
		LOGIN: '/auth/login',
		REGISTER: '/auth/register',
		LOGOUT: '/auth/logout',
		REFRESH: '/auth/refresh',
		ME: '/auth/me'
	},
	PRODUCTS: {
		LIST: '/products',
		DETAIL: (id: string | number) => `/products/${id}`
	},
	CART: {
		GET: '/cart',
		ADD: '/cart/add',
		REMOVE: (id: string | number) => `/cart/${id}`,
		UPDATE: (id: string | number) => `/cart/${id}`
	}
};

const configure = (obj: any): any => {
	if (typeof obj === 'function') return (...args: any[]) => `${API_BASE_URL}${obj(...args)}`;
	if (typeof obj === 'string') return `${API_BASE_URL}${obj}`;
	return Object.fromEntries(Object.entries(obj).map(([k, v]) => [k, configure(v)]));
};

export const ENDPOINTS = configure(ROUTES);

let isRefreshing = false;
let refreshPromise: Promise<boolean> | null = null;

export async function apiFetch(url: string, options: RequestInit & { fetch?: typeof fetch } = {}) {
	const { fetch: customFetch, ...fetchOptions } = options;
	const fetchToUse = customFetch || fetch;

	if (isRefreshing && refreshPromise) {
		await refreshPromise;
	}

	const headers = new Headers(fetchOptions.headers);

	if (auth.accessToken) {
		headers.set('Authorization', `Bearer ${auth.accessToken}`);
	}

	if (fetchOptions.body && !(fetchOptions.body instanceof FormData)) {
		headers.set('Content-Type', 'application/json');
	}

	let response = await fetchToUse(url, {
		...fetchOptions,
		headers
	});

	if ((response.status === 403 || response.status === 401) && auth.refreshToken) {
		if (!isRefreshing) {
			isRefreshing = true;
			refreshPromise = auth.refreshTokens().finally(() => {
				isRefreshing = false;
				refreshPromise = null;
			});
		}

		const success = await refreshPromise;

		if (success && auth.accessToken) {
			headers.set('Authorization', `Bearer ${auth.accessToken}`);
			response = await fetchToUse(url, {
				...fetchOptions,
				headers
			});
		}
	}

	return response;
}
