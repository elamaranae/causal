<script lang="ts">
	import './layout.css';
	import favicon from '$lib/assets/favicon.svg';
	import Logo from '$lib/components/Logo.svelte';
	import Button from '$lib/components/Button.svelte';
	import CartDrawer from '$lib/components/CartDrawer.svelte';
	import { cart } from '$lib/cart.svelte';
	import { auth } from '$lib/auth.svelte';

	let { children } = $props();

	let isMenuOpen = $state(false);
	let isCartOpen = $state(false);

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
		<div class="mx-auto flex h-14 max-w-screen-2xl items-center justify-between px-4 sm:px-6 lg:px-8">
			<div class="flex items-center gap-8">
				<Logo />
			</div>

			<div class="hidden md:flex items-center gap-4">
				<button
					class="relative p-2 text-slate-600 hover:text-slate-900 transition-colors cursor-pointer"
					onclick={toggleCart}
				>
					<svg class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor">
						<path stroke-linecap="round" stroke-linejoin="round" d="M15.75 10.5V6a3.75 3.75 0 10-7.5 0v4.5m11.356-1.993l1.263 12c.07.665-.45 1.243-1.119 1.243H4.25a1.125 1.125 0 01-1.12-1.243l1.264-12A1.125 1.125 0 015.513 7.5h12.974c.576 0 1.059.435 1.119 1.007zM8.625 10.5a.375.375 0 11-.75 0 .375.375 0 01.75 0zm7.5 0a.375.375 0 11-.75 0 .375.375 0 01.75 0z" />
					</svg>
					{#if cart.totalItems > 0}
						<span class="absolute -top-0.5 -right-0.5 inline-flex items-center justify-center w-4 h-4 text-[10px] font-bold text-white bg-slate-900 rounded-full">
							{cart.totalItems}
						</span>
					{/if}
				</button>
				{#if auth.isAuthenticated}
					<span class="text-sm text-slate-600">{auth.user?.email}</span>
					<button
						onclick={handleLogout}
						class="text-sm text-slate-600 hover:text-slate-900 transition-colors cursor-pointer"
					>
						Logout
					</button>
				{:else}
					<a href="/login" class="text-sm text-slate-600 hover:text-slate-900 transition-colors">Log in</a>
					<a href="/register">
						<Button variant="primary" class="!py-1.5 !px-4 !text-sm">Sign up</Button>
					</a>
				{/if}
			</div>

			<!-- Mobile menu button -->
			<div class="md:hidden flex items-center gap-2">
				<button
					class="relative p-2 text-slate-600 hover:text-slate-900 cursor-pointer"
					onclick={toggleCart}
				>
					<svg class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor">
						<path stroke-linecap="round" stroke-linejoin="round" d="M15.75 10.5V6a3.75 3.75 0 10-7.5 0v4.5m11.356-1.993l1.263 12c.07.665-.45 1.243-1.119 1.243H4.25a1.125 1.125 0 01-1.12-1.243l1.264-12A1.125 1.125 0 015.513 7.5h12.974c.576 0 1.059.435 1.119 1.007zM8.625 10.5a.375.375 0 11-.75 0 .375.375 0 01.75 0zm7.5 0a.375.375 0 11-.75 0 .375.375 0 01.75 0z" />
					</svg>
					{#if cart.totalItems > 0}
						<span class="absolute -top-0.5 -right-0.5 inline-flex items-center justify-center w-4 h-4 text-[10px] font-bold text-white bg-slate-900 rounded-full">
							{cart.totalItems}
						</span>
					{/if}
				</button>
				<button
					type="button"
					class="p-2 text-slate-600 hover:text-slate-900 cursor-pointer"
					onclick={() => (isMenuOpen = !isMenuOpen)}
					aria-label="Toggle menu"
				>
					<svg class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor">
						{#if isMenuOpen}
							<path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
						{:else}
							<path stroke-linecap="round" stroke-linejoin="round" d="M3.75 6.75h16.5M3.75 12h16.5m-16.5 5.25h16.5" />
						{/if}
					</svg>
				</button>
			</div>
		</div>

		<!-- Mobile menu -->
		{#if isMenuOpen}
			<div class="md:hidden border-t border-slate-200 bg-white px-4 py-4 space-y-3 shadow-lg">
				<div class="flex flex-col gap-2">
					{#if auth.isAuthenticated}
						<span class="text-sm text-slate-600">{auth.user?.email}</span>
						<button
							onclick={handleLogout}
							class="text-left text-sm text-slate-600 hover:text-slate-900 cursor-pointer"
						>
							Logout
						</button>
					{:else}
						<a href="/login" class="text-sm text-slate-600 hover:text-slate-900" onclick={() => (isMenuOpen = false)}>Log in</a>
						<a href="/register" onclick={() => (isMenuOpen = false)}>
							<Button variant="primary" class="w-full justify-center !py-2 !text-sm">Sign up</Button>
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

	<CartDrawer bind:isOpen={isCartOpen} />
</div>
