<script lang="ts">
	import './layout.css';
	import favicon from '$lib/assets/favicon.svg';
	import Logo from '$lib/components/Logo.svelte';
	import Button from '$lib/components/Button.svelte';
	import CartDrawer from '$lib/components/CartDrawer.svelte';
	import { page } from '$app/state';
	import { cart } from '$lib/cart.svelte';
	import { auth } from '$lib/auth.svelte';

	let { children } = $props();
	
	let isMenuOpen = $state(false);
	let isCartOpen = $state(false);

	function toggleMenu() {
		isMenuOpen = !isMenuOpen;
	}

	function toggleCart() {
		isCartOpen = !isCartOpen;
	}

	async function handleLogout() {
		await auth.logout();
		isMenuOpen = false;
	}
</script>

<svelte:head>
	<link rel="icon" href={favicon} />
	<title>Causal - Modern Essentials</title>
</svelte:head>

<div class="min-h-screen flex flex-col font-sans text-slate-900 bg-white">
	<!-- Navbar -->
	<header class="sticky top-0 z-50 w-full border-b border-slate-200 bg-white/80 backdrop-blur-md">
		<div class="mx-auto flex h-16 max-w-7xl items-center justify-between px-4 sm:px-6 lg:px-8">
			<div class="flex items-center gap-8">
				<Logo />
				<nav class="hidden md:flex items-center gap-6 text-sm font-medium text-slate-600">
					<a href="/" class="hover:text-slate-900 transition-colors">Shop</a>
					<a href="/" class="hover:text-slate-900 transition-colors">New Arrivals</a>
					<a href="/" class="hover:text-slate-900 transition-colors">About</a>
				</nav>
			</div>

			<div class="hidden md:flex items-center gap-4">
				<div class="flex items-center gap-4 mr-4 pr-4 border-r border-slate-200">
					<button 
						class="relative p-2 text-slate-600 hover:text-slate-900 transition-colors cursor-pointer"
						onclick={toggleCart}
					>
						<svg class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor">
							<path stroke-linecap="round" stroke-linejoin="round" d="M15.75 10.5V6a3.75 3.75 0 10-7.5 0v4.5m11.356-1.993l1.263 12c.07.665-.45 1.243-1.119 1.243H4.25a1.125 1.125 0 01-1.12-1.243l1.264-12A1.125 1.125 0 015.513 7.5h12.974c.576 0 1.059.435 1.119 1.007zM8.625 10.5a.375.375 0 11-.75 0 .375.375 0 01.75 0zm7.5 0a.375.375 0 11-.75 0 .375.375 0 01.75 0z" />
						</svg>
						{#if cart.totalItems > 0}
							<span class="absolute top-0 right-0 inline-flex items-center justify-center px-2 py-1 text-xs font-bold leading-none text-white transform translate-x-1/2 -translate-y-1/2 bg-slate-900 rounded-full">
								{cart.totalItems}
							</span>
						{/if}
					</button>
				</div>
				{#if auth.isAuthenticated}
					<div class="flex items-center gap-4">
						<span class="text-sm font-medium text-slate-600">{auth.user?.email}</span>
						<button 
							onclick={handleLogout}
							class="text-sm font-medium text-slate-600 hover:text-slate-900 transition-colors cursor-pointer"
						>
							Logout
						</button>
					</div>
				{:else}
					<a href="/login" class="text-sm font-medium text-slate-600 hover:text-slate-900 transition-colors">Log in</a>
					<a href="/register">
						<Button variant="primary" class="!py-2 !px-4 !text-sm">Sign up</Button>
					</a>
				{/if}
			</div>

			<!-- Mobile menu button -->
			<button 
				type="button"
				class="md:hidden p-2 text-slate-600 hover:text-slate-900 cursor-pointer" 
				onclick={toggleMenu}
				aria-label="Toggle menu"
			>
				<svg class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor">
					{#if isMenuOpen}
						<path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
					{:else}
						<path stroke-linecap="round" stroke-linejoin="round" d="M3.75 6.75h16.5M3.75 12h16.5m-16.5 5.25h16.5" />
					{/if}
				</svg>
			</button>
		</div>

		<!-- Mobile menu -->
		{#if isMenuOpen}
			<div class="md:hidden border-t border-slate-200 bg-white px-4 py-6 space-y-4 shadow-lg">
				<nav class="flex flex-col gap-4 text-base font-medium text-slate-600">
					<a href="/" class="hover:text-slate-900" onclick={() => isMenuOpen = false}>Shop</a>
					<a href="/" class="hover:text-slate-900" onclick={() => isMenuOpen = false}>New Arrivals</a>
					<a href="/" class="hover:text-slate-900" onclick={() => isMenuOpen = false}>About</a>
				</nav>
				<div class="border-t border-slate-100 pt-4 flex flex-col gap-3">
					{#if auth.isAuthenticated}
						<div class="flex flex-col gap-2">
							<span class="text-sm font-medium text-slate-600">{auth.user?.email}</span>
							<button 
								onclick={handleLogout}
								class="text-left w-full py-2 text-slate-600 font-medium hover:text-slate-900 cursor-pointer"
							>
								Logout
							</button>
						</div>
					{:else}
						<a href="/login" class="block text-center w-full py-2 text-slate-600 font-medium hover:text-slate-900" onclick={() => isMenuOpen = false}>Log in</a>
						<a href="/register" onclick={() => isMenuOpen = false} class="block">
							<Button variant="primary" class="w-full justify-center">Sign up</Button>
						</a>
					{/if}
				</div>
			</div>
		{/if}
	</header>

	<!-- Main Content -->
	<main class="flex-1">
		{@render children()}
	</main>

	<!-- Footer -->
	<footer class="border-t border-slate-200 bg-slate-50">
		<div class="mx-auto max-w-7xl px-4 py-12 sm:px-6 lg:px-8">
			<div class="grid grid-cols-1 md:grid-cols-4 gap-8">
				<div class="col-span-1 md:col-span-1">
					<Logo />
					<p class="mt-4 text-sm text-slate-500">
						Thoughtfully designed essentials for your everyday life. Minimal, functional, and built to last.
					</p>
				</div>
				<div>
					<h3 class="text-sm font-semibold text-slate-900">Shop</h3>
					<ul class="mt-4 space-y-2 text-sm text-slate-600">
						<li><a href="/" class="hover:text-slate-900">All Products</a></li>
						<li><a href="/" class="hover:text-slate-900">New Arrivals</a></li>
						<li><a href="/" class="hover:text-slate-900">Accessories</a></li>
					</ul>
				</div>
				<div>
					<h3 class="text-sm font-semibold text-slate-900">Company</h3>
					<ul class="mt-4 space-y-2 text-sm text-slate-600">
						<li><a href="/" class="hover:text-slate-900">About Us</a></li>
						<li><a href="/" class="hover:text-slate-900">Sustainability</a></li>
						<li><a href="/" class="hover:text-slate-900">Terms & Conditions</a></li>
					</ul>
				</div>
				<div>
					<h3 class="text-sm font-semibold text-slate-900">Stay Connected</h3>
					<p class="mt-4 text-sm text-slate-500">
						Subscribe to our newsletter for updates and exclusive offers.
					</p>
					<div class="mt-4 flex gap-2">
						<input type="email" placeholder="Enter your email" class="block w-full rounded-md border-slate-300 shadow-sm focus:border-slate-500 focus:ring-slate-500 sm:text-sm px-3 py-2" />
						<Button variant="primary" class="!py-2 !px-4">Subscribe</Button>
					</div>
				</div>
			</div>
			<div class="mt-12 border-t border-slate-200 pt-8 text-center text-sm text-slate-400">
				&copy; {new Date().getFullYear()} Causal Inc. All rights reserved.
			</div>
		</div>
	</footer>

	<CartDrawer bind:isOpen={isCartOpen} />
</div>
