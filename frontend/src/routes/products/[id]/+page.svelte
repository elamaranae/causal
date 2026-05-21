<script lang="ts">
	import type { PageProps } from './$types';
	import type { Sku, MediaItem } from '$lib/types';
	import Button from '$lib/components/Button.svelte';
	import { cart } from '$lib/cart.svelte';

	let { data }: PageProps = $props();
	let product = $derived(data.product);

	// Extract variant keys, with primaryVariantKey first
	let variantKeys = $derived.by(() => {
		if (!product) return [];
		const keys = new Set<string>();
		for (const sku of product.skus) {
			for (const key of Object.keys(sku.variantAttributes)) {
				keys.add(key);
			}
		}
		const arr = Array.from(keys);
		if (product.primaryVariantKey) {
			const idx = arr.indexOf(product.primaryVariantKey);
			if (idx > 0) {
				arr.splice(idx, 1);
				arr.unshift(product.primaryVariantKey);
			}
		}
		return arr;
	});

	let variantOptions = $derived.by(() => {
		if (!product) return {} as Record<string, string[]>;
		const options: Record<string, Set<string>> = {};
		for (const key of variantKeys) {
			options[key] = new Set<string>();
		}
		for (const sku of product.skus) {
			for (const [key, value] of Object.entries(sku.variantAttributes)) {
				options[key]?.add(value);
			}
		}
		return Object.fromEntries(
			Object.entries(options).map(([k, v]) => [k, Array.from(v)])
		);
	});

	// Current selection state — initialize from the default SKU
	let selection = $state<Record<string, string>>({});

	$effect(() => {
		if (!product) return;
		const defaultSku = product.skus.find((s) => s.id === product.defaultSkuId) ?? product.skus[0];
		if (defaultSku) {
			selection = { ...defaultSku.variantAttributes };
		}
	});

	// Find the SKU matching the current selection
	let selectedSku = $derived.by(() => {
		if (!product) return null;
		return product.skus.find((sku) =>
			variantKeys.every((key) => sku.variantAttributes[key] === selection[key])
		) ?? null;
	});

	// For a given key+value, check if any SKU exists that matches all OTHER selected attributes
	function isOptionAvailable(key: string, value: string): boolean {
		if (!product) return false;
		return product.skus.some((sku) => {
			if (sku.variantAttributes[key] !== value) return false;
			return variantKeys.every((otherKey) => {
				if (otherKey === key) return true;
				return sku.variantAttributes[otherKey] === selection[otherKey];
			});
		});
	}

	function selectOption(key: string, value: string) {
		selection = { ...selection, [key]: value };
	}

	// Media from selected SKU
	let mediaItems = $derived.by((): MediaItem[] => {
		if (!selectedSku?.media) return [];
		return selectedSku.media.media;
	});

	let selectedMediaIndex = $state(0);
	let selectedMedia = $derived(mediaItems[selectedMediaIndex] ?? null);

	// Reset media index when SKU changes
	$effect(() => {
		mediaItems;
		selectedMediaIndex = 0;
	});

	// Merged attributes: product-level + sku-level
	let mergedAttributes = $derived.by(() => {
		if (!product) return {};
		return {
			...product.attributes,
			...(selectedSku?.attributes ?? {})
		};
	});

	function formatKey(key: string): string {
		return key.replace(/_/g, ' ').replace(/\b\w/g, (c) => c.toUpperCase());
	}
</script>

<style>
	.product-image {
		animation: fadeIn 0.3s ease-out;
	}
	@keyframes fadeIn {
		from { opacity: 0.6; }
		to { opacity: 1; }
	}
</style>

{#if !product}
	<div class="min-h-[60vh] flex items-center justify-center">
		<div class="text-center">
			<p class="text-lg text-slate-400 tracking-wide">Product not found</p>
			<a href="/" class="mt-6 inline-block text-sm text-slate-500 underline underline-offset-4 hover:text-slate-800 transition-colors">
				Return to shop
			</a>
		</div>
	</div>
{:else}
	<article class="pb-20">
		<!-- Top bar -->
		<div class="border-b border-slate-100">
			<div class="max-w-screen-xl mx-auto px-6 lg:px-10 py-4">
				<a href="/" class="inline-flex items-center gap-2 text-xs tracking-wide uppercase text-slate-400 hover:text-slate-700 transition-colors">
					<svg class="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor">
						<path stroke-linecap="round" stroke-linejoin="round" d="M10.5 19.5 3 12m0 0 7.5-7.5M3 12h18" />
					</svg>
					All products
				</a>
			</div>
		</div>

		<div class="max-w-screen-xl mx-auto px-6 lg:px-10">
			<div class="grid grid-cols-1 lg:grid-cols-2 gap-0 lg:gap-20 pt-8 lg:pt-14">

				<!-- ===================== LEFT: Gallery ===================== -->
				<div>
					<!-- Main image -->
					<div class="bg-gradient-to-b from-slate-50 to-slate-100/50 rounded-sm overflow-hidden">
						{#key selectedMedia?.url ?? product.primaryThumbnailUrl}
							<div class="product-image aspect-square overflow-hidden">
								{#if selectedMedia}
									<img
										src={selectedMedia.url}
										alt={product.name}
										class="h-full w-full object-cover"
									/>
								{:else if product.primaryThumbnailUrl}
									<img
										src={product.primaryThumbnailUrl}
										alt={product.name}
										class="h-full w-full object-cover"
									/>
								{:else}
									<svg class="w-20 h-20 text-slate-200" fill="none" viewBox="0 0 24 24" stroke-width="0.5" stroke="currentColor">
										<path stroke-linecap="round" stroke-linejoin="round" d="m2.25 15.75 5.159-5.159a2.25 2.25 0 0 1 3.182 0l5.159 5.159m-1.5-1.5 1.409-1.409a2.25 2.25 0 0 1 3.182 0l2.909 2.909M3.75 21h16.5A2.25 2.25 0 0 0 22.5 18.75V5.25A2.25 2.25 0 0 0 20.25 3H3.75A2.25 2.25 0 0 0 1.5 5.25v13.5A2.25 2.25 0 0 0 3.75 21Z" />
									</svg>
								{/if}
							</div>
						{/key}
					</div>

					<!-- Thumbnails -->
					{#if mediaItems.length > 1}
						<div class="flex gap-3 mt-4">
							{#each mediaItems as item, i}
								<button
									class="w-18 h-18 rounded-sm overflow-hidden transition-all cursor-pointer
										{selectedMediaIndex === i
											? 'ring-2 ring-slate-900 ring-offset-2'
											: 'opacity-60 hover:opacity-100'}"
									onclick={() => (selectedMediaIndex = i)}
								>
									<img src={item.thumbnail} alt="" class="h-full w-full object-cover" />
								</button>
							{/each}
						</div>
					{/if}
				</div>

				<!-- ===================== RIGHT: Details ===================== -->
				<div class="pt-8 lg:pt-2">
					<div class="lg:sticky lg:top-20 lg:max-w-md">

						<!-- Name & Price -->
						<header>
							<h1 class="text-2xl sm:text-3xl font-semibold tracking-tight text-slate-900 leading-tight">
								{product.name}
							</h1>
							<p class="mt-4 text-xl tracking-tight h-7">
							{#if selectedSku}
								<span class="text-slate-900">${selectedSku.price.priceAmount.toFixed(2)}</span>
							{:else}
								<span class="text-red-500">Unavailable</span>
							{/if}
						</p>
						</header>

						<!-- Divider -->
						<div class="mt-8 border-t border-slate-100"></div>

						<!-- Variant selectors -->
						{#if variantKeys.length > 0}
							<div class="mt-8 space-y-7">
								{#each variantKeys as key}
									<fieldset>
										<legend class="text-xs font-medium uppercase tracking-widest text-slate-400 mb-3">
											{formatKey(key)}
										</legend>
										<div class="flex flex-wrap gap-2">
											{#each variantOptions[key] as value}
												{@const available = isOptionAvailable(key, value)}
												{@const selected = selection[key] === value}
												<button
													class="group relative h-10 min-w-[4rem] px-4 text-sm tracking-wide rounded-sm border transition-all cursor-pointer
														{selected
															? 'border-slate-900 bg-slate-900 text-white'
															: available
																? 'border-slate-200 text-slate-700 bg-white hover:border-slate-400'
																: 'border-dashed border-slate-200 text-slate-300 bg-white hover:border-slate-300'}"
													onclick={() => selectOption(key, value)}
												>
													<span class={!available && !selected ? 'opacity-50' : ''}>{value}</span>
													{#if !available && !selected}
														<span class="absolute -bottom-5 left-1/2 -translate-x-1/2 text-[10px] text-slate-400 whitespace-nowrap">
															unavailable
														</span>
													{/if}
												</button>
											{/each}
										</div>
									</fieldset>
								{/each}
							</div>
						{/if}

						<!-- Add to cart -->
						<div class="mt-10">
							<button
								class="w-full h-12 text-sm font-medium tracking-wide uppercase rounded-sm transition-all cursor-pointer
									{selectedSku
										? 'bg-slate-900 text-white hover:bg-slate-800 active:scale-[0.99]'
										: 'bg-slate-100 text-slate-400 cursor-not-allowed'}"
								disabled={!selectedSku}
								onclick={() => {
									if (product && selectedSku) {
										cart.addItem({
											id: product.id,
											name: product.name,
											primaryThumbnailUrl: product.primaryThumbnailUrl,
											categoryId: product.categoryId,
											defaultSku: {
												id: selectedSku.id,
												attributes: selectedSku.attributes,
												variantAttributes: selectedSku.variantAttributes,
												price: selectedSku.price
											}
										});
									}
								}}
							>
								{selectedSku ? 'Add to Cart' : 'Unavailable'}
							</button>
						</div>

						<!-- Specifications -->
						{#if Object.keys(mergedAttributes).length > 0}
							<div class="mt-12">
								<div class="border-t border-slate-100 pt-8">
									<h2 class="text-xs font-medium uppercase tracking-widest text-slate-400 mb-5">
										Specifications
									</h2>
									<table class="w-full">
										<tbody>
											{#each Object.entries(mergedAttributes) as [key, value], i}
												<tr class={i > 0 ? 'border-t border-slate-50' : ''}>
													<td class="py-3 pr-8 text-sm text-slate-500 align-top">{formatKey(key)}</td>
													<td class="py-3 text-sm text-slate-900 text-right">{value}</td>
												</tr>
											{/each}
										</tbody>
									</table>
								</div>
							</div>
						{/if}

					</div>
				</div>
			</div>
		</div>
	</article>
{/if}
