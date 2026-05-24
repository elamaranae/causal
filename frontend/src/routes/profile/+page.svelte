<script lang="ts">
  import { onMount } from 'svelte';
  import { goto } from '$app/navigation';
  import { page } from '$app/stores';
  import { auth } from '$lib/auth.svelte';
  import { apiFetch, urls } from '$lib/api';
  import type { Profile, Address } from '$lib/types';
  import Button from '$lib/components/Button.svelte';
  import Input from '$lib/components/Input.svelte';

  type Section = 'profile' | 'addresses';
  let activeSection = $derived<Section>(
    $page.url.searchParams.get('section') === 'addresses' ? 'addresses' : 'profile'
  );

  function setSection(section: Section) {
    goto(section === 'profile' ? '/profile' : '/profile?section=addresses');
  }

  // Profile state
  let profile = $state<Profile | null>(null);
  let profileLoading = $state(true);
  let profileExists = $state(false);
  let profileError = $state<string | null>(null);
  let profileSaving = $state(false);
  let editingProfile = $state(false);

  let firstName = $state('');
  let lastName = $state('');
  let currency = $state('USD');

  // Address state
  let addresses = $state<Address[]>([]);
  let addressesLoading = $state(true);

  // Address form state
  let editingAddress = $state<Address | null>(null);
  let showAddressForm = $state(false);
  let addressSaving = $state(false);
  let addressError = $state<string | null>(null);

  let addrLabel = $state('');
  let addrLine1 = $state('');
  let addrLine2 = $state('');
  let addrCity = $state('');
  let addrState = $state('');
  let addrCountry = $state('');
  let addrPincode = $state('');
  let addrPhone = $state('');

  onMount(() => {
    if (!auth.isAuthenticated) {
      goto('/login');
      return;
    }
    loadProfile();
    loadAddresses();
  });

  async function loadProfile() {
    profileLoading = true;
    try {
      const res = await apiFetch(urls.profile.me);
      if (res.ok) {
        profile = await res.json();
        profileExists = true;
        syncProfileForm();
      } else if (res.status === 404) {
        profileExists = false;
        editingProfile = true;
      }
    } finally {
      profileLoading = false;
    }
  }

  function syncProfileForm() {
    if (!profile) return;
    firstName = profile.firstName;
    lastName = profile.lastName;
    currency = profile.currency;
  }

  function startEditProfile() {
    syncProfileForm();
    editingProfile = true;
  }

  function cancelEditProfile() {
    editingProfile = false;
    profileError = null;
    syncProfileForm();
  }

  async function saveProfile(e: Event) {
    e.preventDefault();
    profileSaving = true;
    profileError = null;
    try {
      const body = JSON.stringify({
        firstName,
        lastName,
        currency,
        ...(profile ? { defaultAddressId: profile.defaultAddressId } : {})
      });
      const res = await apiFetch(urls.profile.me, {
        method: profileExists ? 'PATCH' : 'POST',
        body
      });
      if (res.ok) {
        profile = await res.json();
        profileExists = true;
        editingProfile = false;
        syncProfileForm();
      } else {
        const data = await res.json();
        profileError = data.message || 'Failed to save profile';
      }
    } catch (err: unknown) {
      profileError = err instanceof Error ? err.message : 'Something went wrong';
    } finally {
      profileSaving = false;
    }
  }

  async function setDefaultAddress(addressId: number) {
    const res = await apiFetch(urls.profile.me, {
      method: 'PATCH',
      body: JSON.stringify({ defaultAddressId: addressId })
    });
    if (res.ok) {
      profile = await res.json();
    }
  }

  async function loadAddresses() {
    addressesLoading = true;
    try {
      const res = await apiFetch(urls.profile.addresses);
      if (res.ok) {
        addresses = await res.json();
      }
    } finally {
      addressesLoading = false;
    }
  }

  function openAddressForm(address?: Address) {
    if (address) {
      editingAddress = address;
      addrLabel = address.label || '';
      addrLine1 = address.line1;
      addrLine2 = address.line2 || '';
      addrCity = address.city;
      addrState = address.state;
      addrCountry = address.country;
      addrPincode = address.pincode;
      addrPhone = address.phoneNumber || '';
    } else {
      editingAddress = null;
      addrLabel = '';
      addrLine1 = '';
      addrLine2 = '';
      addrCity = '';
      addrState = '';
      addrCountry = '';
      addrPincode = '';
      addrPhone = '';
    }
    addressError = null;
    showAddressForm = true;
  }

  function closeAddressForm() {
    showAddressForm = false;
    editingAddress = null;
    addressError = null;
  }

  async function saveAddress(e: Event) {
    e.preventDefault();
    addressSaving = true;
    addressError = null;
    try {
      const body = JSON.stringify({
        label: addrLabel || null,
        line1: addrLine1,
        line2: addrLine2 || null,
        city: addrCity,
        state: addrState,
        country: addrCountry,
        pincode: addrPincode,
        phoneNumber: addrPhone || null
      });

      const res = editingAddress
        ? await apiFetch(urls.profile.address(editingAddress.id), { method: 'PATCH', body })
        : await apiFetch(urls.profile.addresses, { method: 'POST', body });

      if (res.ok) {
        closeAddressForm();
        await loadAddresses();
      } else {
        const data = await res.json();
        addressError = data.message || 'Failed to save address';
      }
    } catch (err: unknown) {
      addressError = err instanceof Error ? err.message : 'Something went wrong';
    } finally {
      addressSaving = false;
    }
  }

  async function deleteAddress(id: number) {
    const res = await apiFetch(urls.profile.address(id), { method: 'DELETE' });
    if (res.ok) {
      if (profile?.defaultAddressId === id) {
        profile = { ...profile, defaultAddressId: null };
      }
      await loadAddresses();
    }
  }
</script>

<div class="flex flex-1">
  <!-- Sidebar -->
  <aside class="hidden md:flex flex-col w-56 shrink-0 border-r border-slate-200 bg-slate-50/50 p-6 sticky top-14 h-[calc(100vh-3.5rem)]">
    <a href="/" class="inline-flex items-center gap-2 text-xs tracking-wide uppercase text-slate-400 hover:text-slate-700 transition-colors mb-6">
      <svg class="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor">
        <path stroke-linecap="round" stroke-linejoin="round" d="M10.5 19.5 3 12m0 0 7.5-7.5M3 12h18" />
      </svg>
      Back to shop
    </a>
    <p class="text-xs font-semibold text-slate-400 uppercase tracking-wider mb-3">Account</p>
    <nav class="flex flex-col gap-0.5">
      <button
        class="text-left text-sm py-1.5 px-2 rounded transition-colors cursor-pointer {activeSection === 'profile' ? 'bg-slate-200/70 text-slate-900 font-medium' : 'text-slate-600 hover:bg-slate-100 hover:text-slate-900'}"
        onclick={() => setSection('profile')}
      >
        Profile
      </button>
      <button
        class="text-left text-sm py-1.5 px-2 rounded transition-colors cursor-pointer {activeSection === 'addresses' ? 'bg-slate-200/70 text-slate-900 font-medium' : 'text-slate-600 hover:bg-slate-100 hover:text-slate-900'}"
        onclick={() => setSection('addresses')}
      >
        Addresses
      </button>
    </nav>
  </aside>

  <!-- Mobile section tabs -->
  <div class="md:hidden fixed bottom-0 left-0 right-0 z-40 bg-white border-t border-slate-200 px-4 py-2">
    <div class="flex gap-2">
      <button
        class="flex-1 text-xs font-medium py-1.5 px-3 rounded-full transition-colors cursor-pointer {activeSection === 'profile' ? 'bg-slate-900 text-white' : 'bg-slate-100 text-slate-600'}"
        onclick={() => setSection('profile')}
      >
        Profile
      </button>
      <button
        class="flex-1 text-xs font-medium py-1.5 px-3 rounded-full transition-colors cursor-pointer {activeSection === 'addresses' ? 'bg-slate-900 text-white' : 'bg-slate-100 text-slate-600'}"
        onclick={() => setSection('addresses')}
      >
        Addresses
      </button>
    </div>
  </div>

  <!-- Main content -->
  <div class="flex-1 min-w-0 p-6 lg:p-8 max-w-2xl">
    <!-- Mobile back link -->
    <a href="/" class="md:hidden inline-flex items-center gap-2 text-xs tracking-wide uppercase text-slate-400 hover:text-slate-700 transition-colors mb-6">
      <svg class="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor">
        <path stroke-linecap="round" stroke-linejoin="round" d="M10.5 19.5 3 12m0 0 7.5-7.5M3 12h18" />
      </svg>
      Back to shop
    </a>

    <!-- Profile Section -->
    {#if activeSection === 'profile'}
      <h1 class="text-2xl font-bold tracking-tight text-slate-900 mb-6">Profile</h1>

      {#if profileLoading}
        <p class="text-sm text-slate-500">Loading...</p>
      {:else if profileExists && !editingProfile}
        <!-- Read-only profile view -->
        <div class="bg-white border border-slate-200 rounded-lg p-4">
          <div class="flex items-start justify-between">
            <dl class="space-y-4">
            <div>
              <dt class="text-xs font-medium uppercase tracking-wider text-slate-400">Name</dt>
              <dd class="mt-1 text-sm text-slate-900">{profile!.firstName} {profile!.lastName}</dd>
            </div>
            <div>
              <dt class="text-xs font-medium uppercase tracking-wider text-slate-400">Currency</dt>
              <dd class="mt-1 text-sm text-slate-900">{profile!.currency}</dd>
            </div>
            </dl>
            <button
              class="text-xs text-slate-500 hover:text-slate-700 cursor-pointer shrink-0 ml-4"
              onclick={startEditProfile}
            >
              Edit
            </button>
          </div>
        </div>
      {:else}
        <!-- Profile form (create or edit) -->
        <form class="bg-white border border-slate-200 rounded-lg p-6 space-y-4" onsubmit={saveProfile}>
          <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <Input id="firstName" label="First name" bind:value={firstName} required />
            <Input id="lastName" label="Last name" bind:value={lastName} required />
          </div>
          <div class="max-w-[120px]">
            <Input id="currency" label="Currency" bind:value={currency} required maxlength={3} />
          </div>

          {#if profileError}
            <p class="text-red-600 text-sm">{profileError}</p>
          {/if}

          <div class="flex gap-3 pt-2">
            <Button type="submit" disabled={profileSaving}>
              {profileSaving ? 'Saving...' : profileExists ? 'Save' : 'Create Profile'}
            </Button>
            {#if profileExists}
              <Button variant="outline" type="button" onclick={cancelEditProfile}>Cancel</Button>
            {/if}
          </div>
        </form>
      {/if}
    {/if}

    <!-- Addresses Section -->
    {#if activeSection === 'addresses'}
      <div class="flex items-center justify-between mb-6">
        <h1 class="text-2xl font-bold tracking-tight text-slate-900">Addresses</h1>
        {#if profileExists && !showAddressForm}
          <button
            class="text-sm font-medium text-slate-700 hover:text-slate-900 cursor-pointer"
            onclick={() => openAddressForm()}
          >
            + Add address
          </button>
        {/if}
      </div>

      {#if !profileExists}
        <p class="text-sm text-slate-500">Create your profile first to add addresses.</p>
      {:else if showAddressForm}
        <form class="bg-white border border-slate-200 rounded-lg p-6 space-y-4" onsubmit={saveAddress}>
          <h3 class="text-base font-medium text-slate-900 mb-2">
            {editingAddress ? 'Edit Address' : 'New Address'}
          </h3>

          <Input id="addrLabel" label="Label (e.g., Home, Work)" bind:value={addrLabel} />
          <Input id="addrLine1" label="Address line 1" bind:value={addrLine1} required />
          <Input id="addrLine2" label="Address line 2" bind:value={addrLine2} />
          <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <Input id="addrCity" label="City" bind:value={addrCity} required />
            <Input id="addrState" label="State" bind:value={addrState} required />
          </div>
          <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <Input id="addrCountry" label="Country" bind:value={addrCountry} required />
            <Input id="addrPincode" label="Pincode" bind:value={addrPincode} required />
          </div>
          <Input id="addrPhone" label="Phone number" bind:value={addrPhone} type="tel" />

          {#if addressError}
            <p class="text-red-600 text-sm">{addressError}</p>
          {/if}

          <div class="flex gap-3 pt-2">
            <Button type="submit" disabled={addressSaving}>
              {addressSaving ? 'Saving...' : editingAddress ? 'Update' : 'Add'}
            </Button>
            <Button variant="outline" type="button" onclick={closeAddressForm}>Cancel</Button>
          </div>
        </form>
      {:else if addressesLoading}
        <p class="text-sm text-slate-500">Loading...</p>
      {:else if addresses.length === 0}
        <p class="text-sm text-slate-500">No addresses added yet.</p>
      {:else}
        <div class="space-y-3">
          {#each addresses as address (address.id)}
            {@const isDefault = profile?.defaultAddressId === address.id}
            <div class="bg-white border rounded-lg p-4 {isDefault ? 'border-slate-900' : 'border-slate-200'}">
              <div class="flex items-start justify-between">
                <div>
                  {#if address.label}
                    <span class="text-xs font-semibold uppercase tracking-wider text-slate-400">{address.label}</span>
                  {/if}
                  {#if isDefault}
                    <span class="ml-2 text-[10px] font-semibold uppercase tracking-wider bg-slate-900 text-white px-1.5 py-0.5 rounded">Default</span>
                  {/if}
                  <p class="text-sm text-slate-900 mt-1">{address.line1}</p>
                  {#if address.line2}
                    <p class="text-sm text-slate-600">{address.line2}</p>
                  {/if}
                  <p class="text-sm text-slate-600">{address.city}, {address.state} {address.pincode}</p>
                  <p class="text-sm text-slate-600">{address.country}</p>
                  {#if address.phoneNumber}
                    <p class="text-xs text-slate-400 mt-1">{address.phoneNumber}</p>
                  {/if}
                </div>
                <div class="flex items-center gap-2 shrink-0 ml-4">
                  {#if !isDefault}
                    <button
                      class="text-xs text-slate-500 hover:text-slate-700 cursor-pointer"
                      onclick={() => setDefaultAddress(address.id)}
                    >
                      Set default
                    </button>
                  {/if}
                  <button
                    class="text-xs text-slate-500 hover:text-slate-700 cursor-pointer"
                    onclick={() => openAddressForm(address)}
                  >
                    Edit
                  </button>
                  {#if !isDefault}
                    <button
                      class="text-xs text-red-500 hover:text-red-700 cursor-pointer"
                      onclick={() => deleteAddress(address.id)}
                    >
                      Delete
                    </button>
                  {/if}
                </div>
              </div>
            </div>
          {/each}
        </div>
      {/if}
    {/if}
  </div>
</div>
