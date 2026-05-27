import { redirect } from '@sveltejs/kit';
import { apiFetch, urls } from '$lib/api';
import type { Order } from '$lib/types';
import type { PageLoad } from './$types';

export const load: PageLoad = async ({ url, fetch }) => {
  const orderId = url.searchParams.get('orderId');
  if (!orderId) redirect(302, '/');

  const res = await apiFetch(urls.orders.show(Number(orderId)), { fetch });
  if (!res.ok) redirect(302, '/');

  const order: Order = await res.json();
  return { order };
};
