<script lang="ts">
  import { goto, invalidateAll } from '$app/navigation';
  import { auth } from '$lib/auth.svelte';
  import { cart } from '$lib/cart.svelte';
  import { urls, apiFetch } from '$lib/api';
  import Button from '$lib/components/Button.svelte';
  import Input from '$lib/components/Input.svelte';
  import Logo from '$lib/components/Logo.svelte';

  let email = $state('');
  let password = $state('');
  let confirmPassword = $state('');
  let loading = $state(false);
  let error = $state<string | null>(null);

  async function handleSubmit(e: Event) {
    e.preventDefault();
    
    if (password !== confirmPassword) {
      error = 'Passwords do not match';
      return;
    }

    loading = true;
    error = null;

    try {
      const response = await apiFetch(urls.auth.register, {
        method: 'POST',
        body: JSON.stringify({ email, password })
      });

      if (!response.ok) {
        const data = await response.json();
        throw new Error(data.message || 'Registration failed');
      }

      await auth.onLoginSuccess();
      cart.load();
      await invalidateAll();
      goto('/');
    } catch (err: unknown) {
      error = err instanceof Error ? err.message : 'Something went wrong';
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
      Create your account
    </h2>
    <p class="mt-2 text-center text-sm text-slate-600">
      Already have an account?
      <a href="/login" class="font-medium text-slate-900 hover:underline">
        Sign in
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

        <Input
          id="confirm-password"
          label="Confirm Password"
          type="password"
          bind:value={confirmPassword}
          required
        />

        {#if error}
          <div class="text-red-600 text-sm">
            {error}
          </div>
        {/if}

        <div>
          <Button type="submit" class="w-full" disabled={loading}>
            {loading ? 'Creating account...' : 'Create account'}
          </Button>
        </div>
      </form>
    </div>
  </div>
</div>