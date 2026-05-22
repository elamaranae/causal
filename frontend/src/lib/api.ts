import { auth } from '$lib/auth.svelte';

const BASE = import.meta.env.VITE_API_URL || 'http://localhost:8090';

export const urls = {
  auth: {
    login: `${BASE}/auth/login`,
    register: `${BASE}/auth/register`,
    logout: `${BASE}/auth/logout`,
    refresh: `${BASE}/auth/refresh`,
    me: `${BASE}/auth/me`
  },
  products: {
    trending: `${BASE}/products/trending`,
    categories: `${BASE}/products/categories`,
    filter: (categoryId: number, page = 1, size = 20) =>
      `${BASE}/products/filter?categoryId=${categoryId}&page=${page}&size=${size}`,
    show: (id: number | string) => `${BASE}/products/${id}`,
    skusBulk: `${BASE}/products/skus/bulk`
  },
  cart: {
    me: `${BASE}/cart/me`,
    addItem: `${BASE}/cart/me/items`,
    updateItem: (id: number) => `${BASE}/cart/me/items/${id}`,
    removeItem: (id: number) => `${BASE}/cart/me/items/${id}`
  }
} as const;

let refreshPromise: Promise<boolean> | null = null;

export async function apiFetch(
  url: string,
  options: RequestInit & { fetch?: typeof fetch } = {}
) {
  const { fetch: customFetch, ...init } = options;
  const doFetch = customFetch || fetch;

  if (refreshPromise) await refreshPromise;

  const headers = new Headers(init.headers);
  if (init.body && !(init.body instanceof FormData)) {
    headers.set('Content-Type', 'application/json');
  }

  const method = (init.method || 'GET').toUpperCase();
  if (['POST', 'PUT', 'PATCH', 'DELETE'].includes(method)) {
    const csrf = document.cookie.match(/(?:^|;\s*)csrf_token=([^;]*)/)?.[1];
    if (csrf) headers.set('X-CSRF-Token', csrf);
  }

  let res = await doFetch(url, { ...init, headers, credentials: 'include' });

  if ((res.status === 401 || res.status === 403) && auth.isAuthenticated) {
    if (!refreshPromise) {
      refreshPromise = refreshTokens().finally(() => {
        refreshPromise = null;
      });
    }
    const ok = await refreshPromise;
    if (ok) {
      // Re-read CSRF token after refresh (cookie may have changed)
      if (['POST', 'PUT', 'PATCH', 'DELETE'].includes(method)) {
        const newCsrf = document.cookie.match(/(?:^|;\s*)csrf_token=([^;]*)/)?.[1];
        if (newCsrf) headers.set('X-CSRF-Token', newCsrf);
      }
      res = await doFetch(url, { ...init, headers, credentials: 'include' });
    }
  }

  return res;
}

async function refreshTokens(): Promise<boolean> {
  try {
    const res = await fetch(urls.auth.refresh, { method: 'POST', credentials: 'include' });
    if (res.ok) return true;
    await auth.logout();
    return false;
  } catch {
    await auth.logout();
    return false;
  }
}
