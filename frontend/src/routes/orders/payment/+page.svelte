<script lang="ts">
  import { onMount, onDestroy } from 'svelte';
  import { goto } from '$app/navigation';
  import { apiFetch, urls } from '$lib/api';
  import type { Order, Address, Profile } from '$lib/types';
  import Button from '$lib/components/Button.svelte';
  import Input from '$lib/components/Input.svelte';

  let { data } = $props();
  let order = $derived(data.order);

  type Step = 'address' | 'payment';
  let step = $state<Step>('address');

  // Saved addresses
  let savedAddresses = $state<Address[]>([]);
  let addressesLoading = $state(true);
  let defaultAddressId = $state<number | null>(null);

  // Shipping address selection
  type ShipMode = 'saved' | 'new';
  let shipMode = $state<ShipMode>('saved');
  let selectedShipId = $state<number | null>(null);
  let shipLabel = $state('');
  let shipLine1 = $state('');
  let shipLine2 = $state('');
  let shipCity = $state('');
  let shipState = $state('');
  let shipCountry = $state('');
  let shipPincode = $state('');
  let shipPhone = $state('');
  let saveNewShipAddress = $state(false);

  // Billing
  let sameAsShipping = $state(true);
  type BillMode = 'saved' | 'new';
  let billMode = $state<BillMode>('saved');
  let selectedBillId = $state<number | null>(null);
  let billLabel = $state('');
  let billLine1 = $state('');
  let billLine2 = $state('');
  let billCity = $state('');
  let billState = $state('');
  let billCountry = $state('');
  let billPincode = $state('');
  let billPhone = $state('');
  let saveNewBillAddress = $state(false);

  // Payment
  let cardholderName = $state('');
  let cardNumber = $state('');
  let expiryMonth = $state('');
  let expiryYear = $state('');
  let cvv = $state('');

  let placing = $state(false);
  let error = $state<string | null>(null);
  let addressError = $state<string | null>(null);

  // Polling state
  let polling = $state(false);
  let orderStatus = $state<string | null>(null);
  let pollTimer: ReturnType<typeof setInterval> | null = null;

  // Derived: selected shipping address object
  let selectedShipAddress = $derived(
    savedAddresses.find(a => a.id === selectedShipId) ?? null
  );
  let selectedBillAddress = $derived(
    savedAddresses.find(a => a.id === selectedBillId) ?? null
  );

  onMount(async () => {
    try {
      const [addrRes, profileRes] = await Promise.all([
        apiFetch(urls.profile.addresses),
        apiFetch(urls.profile.me)
      ]);
      if (addrRes.ok) {
        savedAddresses = await addrRes.json();
      }
      if (profileRes.ok) {
        const profile: Profile = await profileRes.json();
        defaultAddressId = profile.defaultAddressId;
      }

      // Pre-select default address, or first saved
      if (savedAddresses.length > 0) {
        const defaultAddr = savedAddresses.find(a => a.id === defaultAddressId);
        selectedShipId = defaultAddr ? defaultAddr.id : savedAddresses[0].id;
        selectedBillId = selectedShipId;
        shipMode = 'saved';
        billMode = 'saved';
      } else {
        shipMode = 'new';
        billMode = 'new';
      }
    } finally {
      addressesLoading = false;
    }
  });

  function getAddressPayload(mode: 'saved' | 'new', selectedAddr: Address | null, line1: string, line2: string, city: string, state: string, country: string, pincode: string, phone: string) {
    if (mode === 'saved' && selectedAddr) {
      return {
        label: selectedAddr.label,
        line1: selectedAddr.line1,
        line2: selectedAddr.line2,
        city: selectedAddr.city,
        state: selectedAddr.state,
        country: selectedAddr.country,
        pincode: selectedAddr.pincode,
        phoneNumber: selectedAddr.phoneNumber
      };
    }
    return {
      label: null,
      line1,
      line2: line2 || null,
      city,
      state,
      country,
      pincode,
      phoneNumber: phone || null
    };
  }

  // Resolved addresses for summary display
  let resolvedShipping = $derived.by(() => {
    if (shipMode === 'saved' && selectedShipAddress) {
      return { line1: selectedShipAddress.line1, line2: selectedShipAddress.line2, city: selectedShipAddress.city, state: selectedShipAddress.state, country: selectedShipAddress.country, pincode: selectedShipAddress.pincode };
    }
    return { line1: shipLine1, line2: shipLine2 || null, city: shipCity, state: shipState, country: shipCountry, pincode: shipPincode };
  });
  let resolvedBilling = $derived.by(() => {
    if (sameAsShipping) return resolvedShipping;
    if (billMode === 'saved' && selectedBillAddress) {
      return { line1: selectedBillAddress.line1, line2: selectedBillAddress.line2, city: selectedBillAddress.city, state: selectedBillAddress.state, country: selectedBillAddress.country, pincode: selectedBillAddress.pincode };
    }
    return { line1: billLine1, line2: billLine2 || null, city: billCity, state: billState, country: billCountry, pincode: billPincode };
  });

  async function saveAddressToProfile(label: string, line1: string, line2: string, city: string, state: string, country: string, pincode: string, phone: string): Promise<Address | null> {
    const res = await apiFetch(urls.profile.addresses, {
      method: 'POST',
      body: JSON.stringify({
        label: label || null,
        line1,
        line2: line2 || null,
        city,
        state,
        country,
        pincode,
        phoneNumber: phone || null
      })
    });
    if (res.ok) {
      const addr: Address = await res.json();
      savedAddresses = [...savedAddresses, addr];
      return addr;
    }
    return null;
  }

  async function continueToPayment(e: Event) {
    e.preventDefault();
    addressError = null;

    // Save new shipping address if requested
    if (shipMode === 'new' && saveNewShipAddress) {
      const addr = await saveAddressToProfile(shipLabel, shipLine1, shipLine2, shipCity, shipState, shipCountry, shipPincode, shipPhone);
      if (!addr) {
        addressError = 'Failed to save shipping address';
        return;
      }
    }

    // Save new billing address if requested
    if (!sameAsShipping && billMode === 'new' && saveNewBillAddress) {
      const addr = await saveAddressToProfile(billLabel, billLine1, billLine2, billCity, billState, billCountry, billPincode, billPhone);
      if (!addr) {
        addressError = 'Failed to save billing address';
        return;
      }
    }

    step = 'payment';
  }

  function backToAddress() {
    step = 'address';
  }

  function startPolling() {
    polling = true;
    orderStatus = 'PAYMENT_INITIATED';
    pollTimer = setInterval(async () => {
      try {
        const res = await apiFetch(urls.orders.status(order.id));
        if (!res.ok) return;
        const data: { id: number; status: string } = await res.json();
        orderStatus = data.status;
        if (data.status !== 'PAYMENT_INITIATED') {
          stopPolling();
        }
      } catch {
        // keep polling
      }
    }, 2000);
  }

  function stopPolling() {
    if (pollTimer) {
      clearInterval(pollTimer);
      pollTimer = null;
    }
  }

  onDestroy(stopPolling);

  async function placeOrder(e: Event) {
    e.preventDefault();
    placing = true;
    error = null;
    try {
      const shipping = getAddressPayload(shipMode, selectedShipAddress, shipLine1, shipLine2, shipCity, shipState, shipCountry, shipPincode, shipPhone);
      const billing = sameAsShipping
        ? shipping
        : getAddressPayload(billMode, selectedBillAddress, billLine1, billLine2, billCity, billState, billCountry, billPincode, billPhone);

      const res = await apiFetch(urls.orders.pay(order.id), {
        method: 'POST',
        body: JSON.stringify({
          paymentMethod: {
            type: 'CARD',
            cardholderName,
            cardNumber,
            expiryMonth,
            expiryYear,
            cvv
          },
          shippingAddress: shipping,
          billingAddress: billing
        })
      });
      if (res.ok) {
        startPolling();
      } else {
        const data = await res.json();
        error = data.message || 'Payment failed';
      }
    } catch (err: unknown) {
      error = err instanceof Error ? err.message : 'Something went wrong';
    } finally {
      placing = false;
    }
  }

  let isTerminal = $derived(orderStatus !== null && orderStatus !== 'PAYMENT_INITIATED');
  let isSuccess = $derived(orderStatus === 'PAYMENT_SUCCESS');
  let isFailed = $derived(isTerminal && !isSuccess);
</script>

<style>
  .checkout-grid {
    display: grid;
    grid-template-columns: 1fr;
    gap: 2.5rem;
    max-width: 72rem;
    margin: 0 auto;
    padding: 2.5rem 1rem;
  }
  @media (min-width: 768px) {
    .checkout-grid {
      grid-template-columns: 1fr 1fr;
    }
  }
  .order-panel {
    background: #f8fafc;
    border: 1px solid #e2e8f0;
    border-radius: 0.75rem;
    padding: 1.5rem;
  }
  .order-item {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    padding: 0.75rem 0;
  }
  .order-item + .order-item {
    border-top: 1px solid #e2e8f0;
  }
  .order-total {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding-top: 1rem;
    margin-top: 0.5rem;
    border-top: 2px solid #cbd5e1;
  }
  .form-panel {
    min-width: 0;
  }
  .step-bar {
    display: flex;
    align-items: center;
    gap: 0.75rem;
    margin-bottom: 2rem;
  }
  .step-circle {
    width: 1.75rem;
    height: 1.75rem;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 0.75rem;
    font-weight: 700;
  }
  .step-circle.active {
    background: #0f172a;
    color: white;
  }
  .step-circle.done {
    background: #22c55e;
    color: white;
  }
  .step-circle.pending {
    background: #e2e8f0;
    color: #94a3b8;
  }
  .step-line {
    flex: 1;
    height: 2px;
    background: #e2e8f0;
  }
  .step-line.filled {
    background: #86efac;
  }
  .field-row {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 0.75rem;
  }
  .field-row-3 {
    display: grid;
    grid-template-columns: 1fr 1fr 1fr;
    gap: 0.75rem;
  }
  .fields {
    display: flex;
    flex-direction: column;
    gap: 0.75rem;
  }
  .section-title {
    font-size: 0.938rem;
    font-weight: 600;
    color: #0f172a;
    margin-bottom: 1rem;
  }
  .section {
    margin-bottom: 1.5rem;
  }
  .addr-summary {
    background: white;
    border: 1px solid #e2e8f0;
    border-radius: 0.5rem;
    padding: 1rem;
    margin-top: 1rem;
  }
  .addr-label {
    font-size: 0.688rem;
    font-weight: 700;
    text-transform: uppercase;
    letter-spacing: 0.05em;
    color: #94a3b8;
    margin-bottom: 0.25rem;
  }
  .addr-text {
    font-size: 0.875rem;
    color: #334155;
    line-height: 1.5;
  }
  .error-box {
    background: #fef2f2;
    border: 1px solid #fecaca;
    border-radius: 0.5rem;
    padding: 0.75rem 1rem;
    margin-bottom: 1rem;
  }
  .error-text {
    color: #b91c1c;
    font-size: 0.875rem;
  }
  .back-link {
    display: inline-flex;
    align-items: center;
    gap: 0.375rem;
    font-size: 0.75rem;
    text-transform: uppercase;
    letter-spacing: 0.05em;
    color: #94a3b8;
    text-decoration: none;
    margin-bottom: 0;
  }
  .back-link:hover {
    color: #334155;
  }
  .billing-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 1rem;
  }
  .same-check {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    font-size: 0.875rem;
    color: #64748b;
    cursor: pointer;
  }
  .text-link {
    display: block;
    text-align: center;
    font-size: 0.875rem;
    color: #64748b;
    cursor: pointer;
    padding: 0.5rem;
    background: none;
    border: none;
    width: 100%;
    margin-top: 0.5rem;
  }
  .text-link:hover {
    color: #334155;
  }
  .edit-link {
    font-size: 0.75rem;
    color: #64748b;
    text-decoration: underline;
    cursor: pointer;
    background: none;
    border: none;
  }
  .edit-link:hover {
    color: #0f172a;
  }
  .success-wrap {
    min-height: 70vh;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 1rem;
  }
  .success-box {
    text-align: center;
    max-width: 20rem;
  }
  .success-icon {
    width: 4rem;
    height: 4rem;
    border-radius: 50%;
    background: #f0fdf4;
    display: flex;
    align-items: center;
    justify-content: center;
    margin: 0 auto 1.25rem;
  }
  .fail-icon {
    width: 4rem;
    height: 4rem;
    border-radius: 50%;
    background: #fef2f2;
    display: flex;
    align-items: center;
    justify-content: center;
    margin: 0 auto 1.25rem;
  }
  .spinner-wrap {
    margin: 0 auto 1.25rem;
    display: flex;
    justify-content: center;
  }
  .spinner {
    width: 3rem;
    height: 3rem;
    border: 3px solid #e2e8f0;
    border-top-color: #0f172a;
    border-radius: 50%;
    animation: spin 0.8s linear infinite;
  }
  @keyframes spin {
    to { transform: rotate(360deg); }
  }
  .item-name {
    font-size: 0.875rem;
    font-weight: 500;
    color: #0f172a;
  }
  .item-desc {
    font-size: 0.75rem;
    color: #64748b;
    margin-top: 0.125rem;
  }
  .item-qty {
    font-size: 0.75rem;
    color: #94a3b8;
    margin-top: 0.25rem;
  }
  .item-price {
    font-size: 0.875rem;
    font-weight: 600;
    color: #0f172a;
    white-space: nowrap;
  }
  .addr-card {
    border: 1px solid #e2e8f0;
    border-radius: 0.5rem;
    padding: 0.75rem 1rem;
    cursor: pointer;
    transition: border-color 0.15s, background 0.15s;
    background: white;
    text-align: left;
  }
  .addr-card:hover {
    border-color: #94a3b8;
  }
  .addr-card.selected {
    border-color: #0f172a;
    background: #f8fafc;
  }
  .addr-card-label {
    font-size: 0.688rem;
    font-weight: 700;
    text-transform: uppercase;
    letter-spacing: 0.05em;
    color: #94a3b8;
  }
  .addr-card-text {
    font-size: 0.813rem;
    color: #334155;
    line-height: 1.4;
    margin-top: 0.25rem;
  }
  .addr-cards {
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
    margin-bottom: 0.75rem;
  }
  .mode-toggle {
    font-size: 0.813rem;
    color: #64748b;
    cursor: pointer;
    background: none;
    border: none;
    padding: 0;
    text-decoration: underline;
  }
  .mode-toggle:hover {
    color: #0f172a;
  }
  .save-check {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    font-size: 0.813rem;
    color: #64748b;
    cursor: pointer;
    margin-top: 0.5rem;
  }
  .default-badge {
    font-size: 0.625rem;
    font-weight: 700;
    text-transform: uppercase;
    letter-spacing: 0.05em;
    background: #0f172a;
    color: white;
    padding: 0.125rem 0.375rem;
    border-radius: 0.25rem;
    margin-left: 0.5rem;
  }
</style>

{#if polling}
  <div class="success-wrap">
    <div class="success-box">
      {#if !isTerminal}
        <!-- Processing -->
        <div class="spinner-wrap">
          <div class="spinner"></div>
        </div>
        <p style="font-size:1.25rem;font-weight:600;color:#0f172a;">Processing payment</p>
        <p style="font-size:0.875rem;color:#64748b;margin-top:0.5rem;">Order #{order.id} — please wait...</p>
      {:else if isSuccess}
        <!-- Completed -->
        <div class="success-icon">
          <svg width="32" height="32" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="#22c55e">
            <path stroke-linecap="round" stroke-linejoin="round" d="M9 12.75 11.25 15 15 9.75M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z" />
          </svg>
        </div>
        <p style="font-size:1.25rem;font-weight:600;color:#0f172a;">Order completed</p>
        <p style="font-size:0.875rem;color:#64748b;margin-top:0.5rem;">Order #{order.id} has been placed successfully.</p>
        <div style="margin-top:1.5rem;">
          <Button variant="outline" onclick={() => goto('/')}>Continue Shopping</Button>
        </div>
      {:else}
        <!-- Failed -->
        <div class="fail-icon">
          <svg width="32" height="32" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="#ef4444">
            <path stroke-linecap="round" stroke-linejoin="round" d="m9.75 9.75 4.5 4.5m0-4.5-4.5 4.5M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z" />
          </svg>
        </div>
        <p style="font-size:1.25rem;font-weight:600;color:#0f172a;">Payment failed</p>
        <p style="font-size:0.875rem;color:#64748b;margin-top:0.5rem;">Order #{order.id} — {orderStatus}</p>
        <div style="margin-top:1.5rem;">
          <Button variant="outline" onclick={() => goto('/')}>Back to Shop</Button>
        </div>
      {/if}
    </div>
  </div>
{:else}
  <div class="checkout-grid">
    <!-- Left: Order Details -->
    <div>
      <a href="/" class="back-link">
        <svg width="14" height="14" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" d="M10.5 19.5 3 12m0 0 7.5-7.5M3 12h18" />
        </svg>
        Back to shop
      </a>

      <h1 style="font-size:1.25rem;font-weight:600;color:#0f172a;margin:1.5rem 0 1rem;">Order details</h1>

      <div class="order-panel">
        {#each order.items as item (item.id)}
          <div class="order-item">
            <div style="min-width:0;flex:1;">
              <p class="item-name">{item.skuName}</p>
              {#if item.skuDescription}
                <p class="item-desc">{item.skuDescription}</p>
              {/if}
              <p class="item-qty">Qty {item.quantity}</p>
            </div>
            <p class="item-price" style="margin-left:1rem;">${item.price.priceAmount.toFixed(2)}</p>
          </div>
        {/each}
        <div class="order-total">
          <span style="font-size:0.875rem;color:#334155;">Total</span>
          <span style="font-size:1.125rem;font-weight:700;color:#0f172a;">${order.total.priceAmount.toFixed(2)}</span>
        </div>
      </div>

      {#if step === 'payment'}
        <div class="addr-summary">
          <div style="display:flex;justify-content:space-between;align-items:center;">
            <p class="addr-label">Shipping address</p>
            <button class="edit-link" onclick={backToAddress}>Edit</button>
          </div>
          <p class="addr-text">{resolvedShipping.line1}{resolvedShipping.line2 ? `, ${resolvedShipping.line2}` : ''}</p>
          <p class="addr-text">{resolvedShipping.city}, {resolvedShipping.state} {resolvedShipping.pincode}</p>
          <p class="addr-text">{resolvedShipping.country}</p>
        </div>

        {#if !sameAsShipping}
          <div class="addr-summary">
            <p class="addr-label">Billing address</p>
            <p class="addr-text">{resolvedBilling.line1}{resolvedBilling.line2 ? `, ${resolvedBilling.line2}` : ''}</p>
            <p class="addr-text">{resolvedBilling.city}, {resolvedBilling.state} {resolvedBilling.pincode}</p>
            <p class="addr-text">{resolvedBilling.country}</p>
          </div>
        {/if}
      {/if}
    </div>

    <!-- Right: Form -->
    <div class="form-panel">
      <div class="step-bar">
        <button
          class="step-circle {step === 'address' ? 'active' : 'done'}"
          onclick={step === 'payment' ? backToAddress : undefined}
          style="cursor:{step === 'payment' ? 'pointer' : 'default'};border:none;"
        >
          {#if step === 'payment'}
            <svg width="14" height="14" fill="none" viewBox="0 0 24 24" stroke-width="2.5" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" d="m4.5 12.75 6 6 9-13.5" />
            </svg>
          {:else}
            1
          {/if}
        </button>
        <span style="font-size:0.875rem;font-weight:500;color:{step === 'address' ? '#0f172a' : '#22c55e'};">Address</span>
        <div class="step-line {step === 'payment' ? 'filled' : ''}"></div>
        <span class="step-circle {step === 'payment' ? 'active' : 'pending'}">2</span>
        <span style="font-size:0.875rem;font-weight:500;color:{step === 'payment' ? '#0f172a' : '#94a3b8'};">Payment</span>
      </div>

      {#if step === 'address'}
        {#if addressesLoading}
          <p style="font-size:0.875rem;color:#64748b;">Loading addresses...</p>
        {:else}
          <form onsubmit={continueToPayment}>
            <div class="section">
              <p class="section-title">Shipping address</p>

              {#if savedAddresses.length > 0}
                {#if shipMode === 'saved'}
                  <div class="addr-cards">
                    {#each savedAddresses as addr (addr.id)}
                      <button
                        type="button"
                        class="addr-card {selectedShipId === addr.id ? 'selected' : ''}"
                        onclick={() => { selectedShipId = addr.id; }}
                      >
                        <div style="display:flex;align-items:center;">
                          {#if addr.label}
                            <span class="addr-card-label">{addr.label}</span>
                          {/if}
                          {#if addr.id === defaultAddressId}
                            <span class="default-badge">Default</span>
                          {/if}
                        </div>
                        <p class="addr-card-text">
                          {addr.line1}{addr.line2 ? `, ${addr.line2}` : ''}<br>
                          {addr.city}, {addr.state} {addr.pincode}<br>
                          {addr.country}
                          {#if addr.phoneNumber}<br>{addr.phoneNumber}{/if}
                        </p>
                      </button>
                    {/each}
                  </div>
                  <button type="button" class="mode-toggle" onclick={() => { shipMode = 'new'; }}>
                    + Use a new address
                  </button>
                {:else}
                  <button type="button" class="mode-toggle" style="margin-bottom:0.75rem;" onclick={() => { shipMode = 'saved'; }}>
                    ← Use a saved address
                  </button>
                  <div class="fields">
                    <Input id="shipLabel" label="Label (e.g., Home, Work)" bind:value={shipLabel} />
                    <Input id="shipLine1" label="Address line 1" bind:value={shipLine1} required />
                    <Input id="shipLine2" label="Address line 2" bind:value={shipLine2} />
                    <div class="field-row">
                      <Input id="shipCity" label="City" bind:value={shipCity} required />
                      <Input id="shipState" label="State" bind:value={shipState} required />
                    </div>
                    <div class="field-row">
                      <Input id="shipCountry" label="Country" bind:value={shipCountry} required />
                      <Input id="shipPincode" label="Pincode" bind:value={shipPincode} required />
                    </div>
                    <Input id="shipPhone" label="Phone number" bind:value={shipPhone} type="tel" />
                    <label class="save-check">
                      <input type="checkbox" bind:checked={saveNewShipAddress} />
                      Save this address to my profile
                    </label>
                  </div>
                {/if}
              {:else}
                <div class="fields">
                  <Input id="shipLabel" label="Label (e.g., Home, Work)" bind:value={shipLabel} />
                  <Input id="shipLine1" label="Address line 1" bind:value={shipLine1} required />
                  <Input id="shipLine2" label="Address line 2" bind:value={shipLine2} />
                  <div class="field-row">
                    <Input id="shipCity" label="City" bind:value={shipCity} required />
                    <Input id="shipState" label="State" bind:value={shipState} required />
                  </div>
                  <div class="field-row">
                    <Input id="shipCountry" label="Country" bind:value={shipCountry} required />
                    <Input id="shipPincode" label="Pincode" bind:value={shipPincode} required />
                  </div>
                  <Input id="shipPhone" label="Phone number" bind:value={shipPhone} type="tel" />
                  <label class="save-check">
                    <input type="checkbox" bind:checked={saveNewShipAddress} />
                    Save this address to my profile
                  </label>
                </div>
              {/if}
            </div>

            <div class="section">
              <div class="billing-header">
                <p class="section-title" style="margin-bottom:0;">Billing address</p>
                <label class="same-check">
                  <input type="checkbox" bind:checked={sameAsShipping} />
                  Same as shipping
                </label>
              </div>
              {#if !sameAsShipping}
                {#if savedAddresses.length > 0}
                  {#if billMode === 'saved'}
                    <div class="addr-cards">
                      {#each savedAddresses as addr (addr.id)}
                        <button
                          type="button"
                          class="addr-card {selectedBillId === addr.id ? 'selected' : ''}"
                          onclick={() => { selectedBillId = addr.id; }}
                        >
                          <div style="display:flex;align-items:center;">
                            {#if addr.label}
                              <span class="addr-card-label">{addr.label}</span>
                            {/if}
                            {#if addr.id === defaultAddressId}
                              <span class="default-badge">Default</span>
                            {/if}
                          </div>
                          <p class="addr-card-text">
                            {addr.line1}{addr.line2 ? `, ${addr.line2}` : ''}<br>
                            {addr.city}, {addr.state} {addr.pincode}<br>
                            {addr.country}
                          </p>
                        </button>
                      {/each}
                    </div>
                    <button type="button" class="mode-toggle" onclick={() => { billMode = 'new'; }}>
                      + Use a new address
                    </button>
                  {:else}
                    <button type="button" class="mode-toggle" style="margin-bottom:0.75rem;" onclick={() => { billMode = 'saved'; }}>
                      ← Use a saved address
                    </button>
                    <div class="fields">
                      <Input id="billLabel" label="Label (e.g., Home, Work)" bind:value={billLabel} />
                      <Input id="billLine1" label="Address line 1" bind:value={billLine1} required />
                      <Input id="billLine2" label="Address line 2" bind:value={billLine2} />
                      <div class="field-row">
                        <Input id="billCity" label="City" bind:value={billCity} required />
                        <Input id="billState" label="State" bind:value={billState} required />
                      </div>
                      <div class="field-row">
                        <Input id="billCountry" label="Country" bind:value={billCountry} required />
                        <Input id="billPincode" label="Pincode" bind:value={billPincode} required />
                      </div>
                      <Input id="billPhone" label="Phone number" bind:value={billPhone} type="tel" />
                      <label class="save-check">
                        <input type="checkbox" bind:checked={saveNewBillAddress} />
                        Save this address to my profile
                      </label>
                    </div>
                  {/if}
                {:else}
                  <div class="fields">
                    <Input id="billLabel" label="Label (e.g., Home, Work)" bind:value={billLabel} />
                    <Input id="billLine1" label="Address line 1" bind:value={billLine1} required />
                    <Input id="billLine2" label="Address line 2" bind:value={billLine2} />
                    <div class="field-row">
                      <Input id="billCity" label="City" bind:value={billCity} required />
                      <Input id="billState" label="State" bind:value={billState} required />
                    </div>
                    <div class="field-row">
                      <Input id="billCountry" label="Country" bind:value={billCountry} required />
                      <Input id="billPincode" label="Pincode" bind:value={billPincode} required />
                    </div>
                    <Input id="billPhone" label="Phone number" bind:value={billPhone} type="tel" />
                    <label class="save-check">
                      <input type="checkbox" bind:checked={saveNewBillAddress} />
                      Save this address to my profile
                    </label>
                  </div>
                {/if}
              {/if}
            </div>

            {#if addressError}
              <div class="error-box">
                <p class="error-text">{addressError}</p>
              </div>
            {/if}

            <Button type="submit" class="w-full">Continue to payment</Button>
          </form>
        {/if}
      {/if}

      {#if step === 'payment'}
        <form onsubmit={placeOrder}>
          <div class="section">
            <p class="section-title">Payment details</p>
            <div class="fields">
              <Input id="cardholderName" label="Cardholder name" bind:value={cardholderName} required />
              <Input id="cardNumber" label="Card number" bind:value={cardNumber} required maxlength={19} />
              <div class="field-row-3">
                <Input id="expiryMonth" label="Month (MM)" bind:value={expiryMonth} required maxlength={2} />
                <Input id="expiryYear" label="Year (YY)" bind:value={expiryYear} required maxlength={2} />
                <Input id="cvv" label="CVV" bind:value={cvv} required maxlength={4} />
              </div>
            </div>
          </div>

          {#if error}
            <div class="error-box">
              <p class="error-text">{error}</p>
            </div>
          {/if}

          <Button type="submit" class="w-full" disabled={placing}>
            {placing ? 'Processing...' : `Place order · $${order.total.priceAmount.toFixed(2)}`}
          </Button>
          <button type="button" class="text-link" onclick={backToAddress}>
            Back to address
          </button>
        </form>
      {/if}
    </div>
  </div>
{/if}
