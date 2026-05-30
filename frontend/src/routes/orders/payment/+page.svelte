<script lang="ts">
  import { onDestroy } from 'svelte';
  import { goto } from '$app/navigation';
  import { apiFetch, urls } from '$lib/api';
  import type { Order } from '$lib/types';
  import Button from '$lib/components/Button.svelte';
  import Input from '$lib/components/Input.svelte';

  let { data } = $props();
  let order = $derived(data.order);

  type Step = 'address' | 'payment';
  let step = $state<Step>('address');

  let shipLine1 = $state('');
  let shipLine2 = $state('');
  let shipCity = $state('');
  let shipState = $state('');
  let shipCountry = $state('');
  let shipPincode = $state('');
  let shipPhone = $state('');

  let sameAsShipping = $state(true);
  let billLine1 = $state('');
  let billLine2 = $state('');
  let billCity = $state('');
  let billState = $state('');
  let billCountry = $state('');
  let billPincode = $state('');
  let billPhone = $state('');

  let cardholderName = $state('');
  let cardNumber = $state('');
  let expiryMonth = $state('');
  let expiryYear = $state('');
  let cvv = $state('');

  let placing = $state(false);
  let error = $state<string | null>(null);

  // Polling state
  let polling = $state(false);
  let orderStatus = $state<string | null>(null);
  let pollTimer: ReturnType<typeof setInterval> | null = null;

  function continueToPayment(e: Event) {
    e.preventDefault();
    step = 'payment';
  }

  function backToAddress() {
    step = 'address';
  }

  function buildAddress(line1: string, line2: string, city: string, state: string, country: string, pincode: string, phone: string) {
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
      const shipping = buildAddress(shipLine1, shipLine2, shipCity, shipState, shipCountry, shipPincode, shipPhone);
      const billing = sameAsShipping
        ? shipping
        : buildAddress(billLine1, billLine2, billCity, billState, billCountry, billPincode, billPhone);

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
    border-bottom: 1px solid #e2e8f0;
  }
  .order-item:last-child {
    border-bottom: none;
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
          <p class="addr-text">{shipLine1}{shipLine2 ? `, ${shipLine2}` : ''}</p>
          <p class="addr-text">{shipCity}, {shipState} {shipPincode}</p>
          <p class="addr-text">{shipCountry}</p>
        </div>

        {#if !sameAsShipping}
          <div class="addr-summary">
            <p class="addr-label">Billing address</p>
            <p class="addr-text">{billLine1}{billLine2 ? `, ${billLine2}` : ''}</p>
            <p class="addr-text">{billCity}, {billState} {billPincode}</p>
            <p class="addr-text">{billCountry}</p>
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
        <form onsubmit={continueToPayment}>
          <div class="section">
            <p class="section-title">Shipping address</p>
            <div class="fields">
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
            </div>
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
              <div class="fields">
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
              </div>
            {/if}
          </div>

          <Button type="submit" class="w-full">Continue to payment</Button>
        </form>
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
