<script lang="ts">
	import Button from '$lib/components/Button.svelte';
	import { cart } from '$lib/cart.svelte';
	import type { PageProps } from './$types';
	
	let { data }: PageProps = $props();

	const productImages = [
		'bg-slate-200',
		'bg-slate-300',
		'bg-slate-400',
		'bg-slate-500',
	];
</script>

<div class="flex flex-col">
	<!-- Hero Section -->
	<section class="relative bg-slate-900 text-white">
		<div class="absolute inset-0 overflow-hidden">
			<!-- Abstract Background -->
			<div class="absolute inset-0 bg-gradient-to-br from-slate-800 to-slate-900"></div>
			<div class="absolute top-0 left-0 w-full h-full opacity-30 bg-[radial-gradient(#ffffff_1px,transparent_1px)] [background-size:20px_20px]"></div>
		</div>
		<div class="relative mx-auto max-w-7xl px-4 py-24 sm:px-6 lg:px-8 lg:py-32 flex flex-col items-center text-center">
			<h1 class="text-4xl font-bold tracking-tight sm:text-5xl lg:text-6xl max-w-3xl">
				Essentials for the modern minimalist.
			</h1>
			<p class="mt-6 text-xl text-slate-300 max-w-2xl">
				Thoughtfully designed clothing that blends comfort with timeless style. Built to last, designed to be lived in.
			</p>
			<div class="mt-10 flex gap-4">
				<Button variant="secondary" class="!px-8 !py-4 !text-lg">Shop Collection</Button>
				<Button variant="outline" class="!px-8 !py-4 !text-lg !bg-transparent !text-white !border-white hover:!bg-white hover:!text-slate-900">Read Our Story</Button>
			</div>
		</div>
	</section>

	<!-- Featured Collection -->
	<section class="mx-auto max-w-7xl px-4 py-16 sm:px-6 lg:px-8">
		<div class="flex items-center justify-between mb-8">
			<h2 class="text-2xl font-bold tracking-tight text-slate-900">Featured Collection</h2>
			<a href="/" class="text-sm font-medium text-slate-600 hover:text-slate-900 flex items-center gap-1">
				View all
				<svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
					<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 8l4 4m0 0l-4 4m4-4H3" />
				</svg>
			</a>
		</div>

		<div class="grid grid-cols-1 gap-y-10 gap-x-6 sm:grid-cols-2 lg:grid-cols-4 xl:gap-x-8">
			{#each data.products as product, i (product.id)}
				<div class="group relative flex flex-col">
					<div class="aspect-h-1 aspect-w-1 w-full overflow-hidden rounded-md {productImages[i % productImages.length]} lg:aspect-none lg:h-80 group-hover:opacity-75 transition-opacity">
						<!-- Placeholder for image -->
						<div class="h-full w-full flex items-center justify-center text-slate-500 font-medium">
							Product Image
						</div>
					</div>
					<div class="mt-4 flex flex-1 flex-col justify-between">
						<div>
							<div class="flex justify-between items-start">
								<h3 class="text-sm font-medium text-slate-700">
									<a href="/">
										<span aria-hidden="true" class="absolute inset-0"></span>
										{product.name}
									</a>
								</h3>
								<p class="text-sm font-medium text-slate-900">${product.price.toFixed(2)}</p>
							</div>
							<p class="mt-1 text-sm text-slate-500">{product.description}</p>
						</div>
						<div class="mt-4">
							<Button 
								variant="primary" 
								class="w-full relative z-10"
								onclick={() => cart.addItem(product)}
							>
								Add to Cart
							</Button>
						</div>
					</div>
				</div>
			{/each}
		</div>
	</section>

	<!-- Call to Action -->
	<section class="bg-slate-50">
		<div class="mx-auto max-w-7xl px-4 py-16 sm:px-6 lg:px-8">
			<div class="bg-slate-900 rounded-2xl overflow-hidden shadow-xl lg:grid lg:grid-cols-2 lg:gap-4">
				<div class="px-6 pt-10 pb-12 sm:px-16 sm:pt-16 lg:py-16 lg:pr-0 xl:py-20 xl:px-20">
					<div class="lg:self-center">
						<h2 class="text-3xl font-bold tracking-tight text-white sm:text-4xl">
							<span class="block">Ready to upgrade your wardrobe?</span>
						</h2>
						<p class="mt-4 text-lg leading-6 text-slate-300">
							Join thousands of others who have simplified their daily routine with our capsule collection.
						</p>
						<a href="/register" class="mt-8 inline-block bg-white border border-transparent rounded-md py-3 px-8 text-base font-medium text-slate-900 hover:bg-slate-100">
							Get started
						</a>
					</div>
				</div>
				<div class="aspect-w-5 aspect-h-3 -mt-6 md:aspect-w-2 md:aspect-h-1">
					<div class="transform translate-x-6 translate-y-6 rounded-md object-cover object-left-top sm:translate-x-16 lg:translate-y-20 bg-slate-800 h-full w-full opacity-50"></div>
				</div>
			</div>
		</div>
	</section>
</div>

