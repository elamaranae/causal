<script lang="ts">
	import { goto } from '$app/navigation';
	import { auth } from '$lib/auth.svelte';
	import { ENDPOINTS, apiFetch } from '$lib/api/config';
	import Button from '$lib/components/Button.svelte';
	import Input from '$lib/components/Input.svelte';
	import Logo from '$lib/components/Logo.svelte';

	let email = $state('');
	let password = $state('');
	let loading = $state(false);
	let error = $state<string | null>(null);

	async function handleSubmit(e: Event) {
		e.preventDefault();
		loading = true;
		error = null;

		try {
			const response = await apiFetch(ENDPOINTS.AUTH.LOGIN, {
				method: 'POST',
				body: JSON.stringify({ email, password })
			});

			if (!response.ok) {
				const data = await response.json();
				throw new Error(data.message || 'Login failed');
			}

			await auth.onLoginSuccess();
			goto('/');
		} catch (err: any) {
			error = err.message;
		} finally {
			loading = false;
		}
	}
</script>

<div class="min-h-screen bg-slate-50 flex flex-col justify-center py-12 sm:px-6 lg:px-8">
	<div class="sm:mx-auto sm:w-full sm:max-w-md">
		<div class="flex justify-center">
			<Logo />
		</div>
		<h2 class="mt-6 text-center text-3xl font-extrabold text-slate-900">
			Sign in to your account
		</h2>
		<p class="mt-2 text-center text-sm text-slate-600">
			Or
			<a href="/register" class="font-medium text-slate-900 hover:underline">
				create a new account
			</a>
		</p>
	</div>

	<div class="mt-8 sm:mx-auto sm:w-full sm:max-w-md">
		<div class="bg-white py-8 px-4 shadow sm:rounded-lg sm:px-10">
			<form class="space-y-6" onsubmit={handleSubmit}>
				<Input
					id="email"
					label="Email address"
					type="email"
					bind:value={email}
					required
				/>

				<Input
					id="password"
					label="Password"
					type="password"
					bind:value={password}
					required
				/>

				{#if error}
					<div class="text-red-600 text-sm">
						{error}
					</div>
				{/if}

				<div>
					<Button type="submit" class="w-full" disabled={loading}>
						{loading ? 'Signing in...' : 'Sign in'}
					</Button>
				</div>
			</form>
		</div>
	</div>
</div>