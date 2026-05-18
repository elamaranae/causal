<script lang="ts">
	import { cart } from '$lib/cart.svelte';
	import { categories } from '$lib/categories.svelte';
	import { ENDPOINTS, apiFetch } from '$lib/api/config';
	import type { ProductListing, Page } from '$lib/types';
	import type { PageProps } from './$types';
	import { onMount } from 'svelte';

	let { data }: PageProps = $props();

	let filteredPage = $state<Page<ProductListing> | null>(null);
	let loading = $state(false);
	let currentPage = $state(1);

	let products = $derived(filteredPage ? filteredPage.content : data.products);
	let showPagination = $derived(filteredPage !== null && filteredPage.totalPages > 1);

	let heading = $derived(
		categories.selectedId !== null
			? categories.categories.find((c) => c.id === categories.selectedId)?.name ?? 'Products'
			: 'Trending'
	);

	onMount(() => {
		categories.load();
	});

	$effect(() => {
		const id = categories.selectedId;
		if (id === null) {
			filteredPage = null;
			currentPage = 1;
		} else {
			fetchFiltered(id, 1);
		}
	});

	async function fetchFiltered(categoryId: number, page: number) {
		loading = true;
		currentPage = page;
		const response = await apiFetch(ENDPOINTS.PRODUCTS.FILTER(categoryId, page));
		if (response.ok) {
			filteredPage = await response.json();
		}
		loading = false;
	}

	function goToPage(page: number) {
		if (categories.selectedId !== null) {
			fetchFiltered(categories.selectedId, page);
		}
	}

	function getCategoryName(categoryId: number): string | undefined {
		return categories.categories.find((c) => c.id === categoryId)?.name;
	}
</script>

<div class="flex flex-1">
	<!-- Desktop category sidebar -->
	<aside class="hidden md:flex flex-col w-56 shrink-0 border-r border-slate-200 bg-slate-50/50 p-6 sticky top-14 h-[calc(100vh-3.5rem)] overflow-y-auto">
		<p class="text-xs font-semibold text-slate-400 uppercase tracking-wider mb-3">Categories</p>
		<nav class="flex flex-col gap-0.5">
			<button
				class="text-left text-sm py-1.5 px-2 rounded transition-colors {categories.selectedId === null ? 'bg-slate-200/70 text-slate-900 font-medium' : 'text-slate-600 hover:bg-slate-100 hover:text-slate-900'} cursor-pointer"
				onclick={() => categories.select(null)}
			>
				All
			</button>
			{#each categories.topLevel as cat (cat.id)}
				<button
					class="text-left text-sm py-1.5 px-2 rounded transition-colors {categories.selectedId === cat.id ? 'bg-slate-200/70 text-slate-900 font-medium' : 'text-slate-600 hover:bg-slate-100 hover:text-slate-900'} cursor-pointer"
					onclick={() => categories.select(cat.id)}
				>
					{cat.name}
				</button>
				{#each categories.childrenOf(cat.id) as child (child.id)}
					<button
						class="text-left text-[13px] py-1 pl-5 pr-2 rounded transition-colors {categories.selectedId === child.id ? 'bg-slate-200/70 text-slate-900 font-medium' : 'text-slate-500 hover:bg-slate-100 hover:text-slate-900'} cursor-pointer"
						onclick={() => categories.select(child.id)}
					>
						{child.name}
					</button>
				{/each}
			{/each}
		</nav>
	</aside>

	<!-- Mobile category bar -->
	{#if categories.topLevel.length > 0}
		<div class="md:hidden fixed bottom-0 left-0 right-0 z-40 bg-white border-t border-slate-200 px-4 py-2 overflow-x-auto">
			<div class="flex gap-2">
				<button
					class="shrink-0 text-xs font-medium py-1.5 px-3 rounded-full transition-colors {categories.selectedId === null ? 'bg-slate-900 text-white' : 'bg-slate-100 text-slate-600'} cursor-pointer"
					onclick={() => categories.select(null)}
				>
					All
				</button>
				{#each categories.categories as cat (cat.id)}
					<button
						class="shrink-0 text-xs font-medium py-1.5 px-3 rounded-full transition-colors {categories.selectedId === cat.id ? 'bg-slate-900 text-white' : 'bg-slate-100 text-slate-600'} cursor-pointer"
						onclick={() => categories.select(cat.id)}
					>
						{cat.name}
					</button>
				{/each}
			</div>
		</div>
	{/if}

	<!-- Product grid -->
	<div class="flex-1 min-w-0 p-6 lg:p-8 max-w-6xl">
		<div class="mb-8">
			<h1 class="text-2xl font-bold tracking-tight text-slate-900">{heading}</h1>
			<p class="mt-1 text-sm text-slate-500">
				{#if categories.selectedId === null}
					Our most popular products right now
				{:else if filteredPage}
					{filteredPage.totalElements} product{filteredPage.totalElements !== 1 ? 's' : ''} in {heading.toLowerCase()}
				{:else}
					Browsing {heading.toLowerCase()}
				{/if}
			</p>
		</div>

		{#if loading}
			<div class="text-center py-16">
				<p class="text-slate-400">Loading...</p>
			</div>
		{:else if products.length === 0}
			<div class="text-center py-16">
				<p class="text-slate-400">No products found.</p>
			</div>
		{:else}
			<div class="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
				{#each products as product (product.id)}
					<div class="group flex flex-col bg-white border border-slate-200 rounded-lg overflow-hidden hover:shadow-md transition-shadow">
						<div class="aspect-square bg-slate-100 overflow-hidden">
							{#if product.primaryThumbnailUrl}
								<img
									src={product.primaryThumbnailUrl}
									alt={product.name}
									class="h-full w-full object-cover group-hover:scale-105 transition-transform duration-300"
								/>
							{:else}
								<div class="h-full w-full flex items-center justify-center text-slate-300">
									<svg class="w-12 h-12" fill="none" viewBox="0 0 24 24" stroke-width="1" stroke="currentColor">
										<path stroke-linecap="round" stroke-linejoin="round" d="m2.25 15.75 5.159-5.159a2.25 2.25 0 0 1 3.182 0l5.159 5.159m-1.5-1.5 1.409-1.409a2.25 2.25 0 0 1 3.182 0l2.909 2.909M3.75 21h16.5A2.25 2.25 0 0 0 22.5 18.75V5.25A2.25 2.25 0 0 0 20.25 3H3.75A2.25 2.25 0 0 0 1.5 5.25v13.5A2.25 2.25 0 0 0 3.75 21Z" />
									</svg>
								</div>
							{/if}
						</div>
						<div class="p-4 flex flex-col gap-2 flex-1">
							<h3 class="text-sm font-medium text-slate-900">{product.name}</h3>
							{#if getCategoryName(product.categoryId)}
								<span class="text-xs text-slate-400">{getCategoryName(product.categoryId)}</span>
							{/if}
							<div class="mt-auto pt-3">
								<button
									class="w-full text-center text-sm font-medium py-2 px-4 rounded-md border border-slate-200 text-slate-700 hover:bg-slate-50 transition-colors cursor-pointer"
									onclick={() => cart.addItem(product)}
								>
									Add to Cart
								</button>
							</div>
						</div>
					</div>
				{/each}
			</div>

			<!-- Pagination -->
			{#if showPagination && filteredPage}
				<div class="mt-8 flex items-center justify-center gap-2">
					<button
						class="px-3 py-1.5 text-sm rounded-md border border-slate-200 text-slate-600 hover:bg-slate-50 transition-colors cursor-pointer disabled:opacity-40 disabled:cursor-not-allowed"
						disabled={filteredPage.first}
						onclick={() => goToPage(currentPage - 1)}
					>
						Previous
					</button>

					{#each Array(filteredPage.totalPages) as _, i}
						<button
							class="w-9 h-9 text-sm rounded-md transition-colors cursor-pointer {currentPage === i + 1 ? 'bg-slate-900 text-white' : 'border border-slate-200 text-slate-600 hover:bg-slate-50'}"
							onclick={() => goToPage(i + 1)}
						>
							{i + 1}
						</button>
					{/each}

					<button
						class="px-3 py-1.5 text-sm rounded-md border border-slate-200 text-slate-600 hover:bg-slate-50 transition-colors cursor-pointer disabled:opacity-40 disabled:cursor-not-allowed"
						disabled={filteredPage.last}
						onclick={() => goToPage(currentPage + 1)}
					>
						Next
					</button>
				</div>
			{/if}
		{/if}
	</div>
</div>
