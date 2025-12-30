<script lang="ts">
	import { cart } from '$lib/cart.svelte';
	import Button from './Button.svelte';
	import { fade, fly } from 'svelte/transition';

	let { isOpen = $bindable(false) }: { isOpen: boolean } = $props();

	function close() {
		isOpen = false;
	}
</script>

{#if isOpen}
	<div class="relative z-[100]" aria-labelledby="slide-over-title" role="dialog" aria-modal="true">
		<!-- Backdrop -->
		<div 
			class="fixed inset-0 bg-slate-500/75 transition-opacity" 
			transition:fade={{ duration: 200 }}
			onclick={close}
			onkeydown={(e) => e.key === 'Escape' && close()}
			tabindex="-1"
		></div>

		<div class="fixed inset-0 overflow-hidden">
			<div class="absolute inset-0 overflow-hidden">
				<div class="pointer-events-none fixed inset-y-0 right-0 flex max-w-full pl-10">
					<div 
						class="pointer-events-auto w-screen max-w-md"
						transition:fly={{ x: 400, duration: 300 }}
					>
						<div class="flex h-full flex-col overflow-y-scroll bg-white shadow-xl">
							<div class="flex-1 overflow-y-auto px-4 py-6 sm:px-6">
								<div class="flex items-start justify-between">
									<h2 class="text-lg font-medium text-slate-900" id="slide-over-title">Shopping cart</h2>
									<div class="ml-3 flex h-7 items-center">
										<button 
											type="button" 
											class="relative -m-2 p-2 text-slate-400 hover:text-slate-500 cursor-pointer"
											onclick={close}
										>
											<span class="absolute -inset-0.5"></span>
											<span class="sr-only">Close panel</span>
											<svg class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor">
												<path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
											</svg>
										</button>
									</div>
								</div>

								<div class="mt-8">
									<div class="flow-root">
										{#if cart.items.length === 0}
											<div class="text-center py-12">
												<p class="text-slate-500">Your cart is empty</p>
												<Button variant="outline" class="mt-4" onclick={close}>Continue Shopping</Button>
											</div>
										{:else}
											<ul role="list" class="-my-6 divide-y divide-slate-200">
												{#each cart.items as item (item.product.id)}
													<li class="flex py-6">
														<div class="h-24 w-24 flex-shrink-0 overflow-hidden rounded-md border border-slate-200 bg-slate-100 flex items-center justify-center text-xs text-slate-400">
															Image
														</div>

														<div class="ml-4 flex flex-1 flex-col">
															<div>
																<div class="flex justify-between text-base font-medium text-slate-900">
																	<h3>{item.product.name}</h3>
																	<p class="ml-4">${(item.product.price * item.quantity).toFixed(2)}</p>
																</div>
																<p class="mt-1 text-sm text-slate-500 line-clamp-1">{item.product.description}</p>
															</div>
															<div class="flex flex-1 items-end justify-between text-sm">
																<div class="flex items-center border border-slate-200 rounded-md h-8">
																	<button 
																		class="px-2 h-full hover:bg-slate-100 text-slate-600 transition-colors border-r border-slate-200 cursor-pointer"
																		onclick={() => cart.updateQuantity(item.product.id, item.quantity - 1)}
																	>
																		-
																	</button>
																	<span class="px-3 font-medium">{item.quantity}</span>
																	<button 
																		class="px-2 h-full hover:bg-slate-100 text-slate-600 transition-colors border-l border-slate-200 cursor-pointer"
																		onclick={() => cart.addItem(item.product)}
																	>
																		+
																	</button>
																</div>

																<div class="flex">
																	<button 
																		type="button" 
																		class="font-medium text-slate-600 hover:text-slate-500 cursor-pointer"
																		onclick={() => cart.removeItem(item.product.id)}
																	>
																		Remove
																	</button>
																</div>
															</div>
														</div>
													</li>
												{/each}
											</ul>
										{/if}
									</div>
								</div>
							</div>

							{#if cart.items.length > 0}
								<div class="border-t border-slate-200 px-4 py-6 sm:px-6">
									<div class="flex justify-between text-base font-medium text-slate-900">
										<p>Subtotal</p>
										<p>${cart.totalPrice.toFixed(2)}</p>
									</div>
									<p class="mt-0.5 text-sm text-slate-500">Shipping and taxes calculated at checkout.</p>
									<div class="mt-6">
										<Button variant="primary" class="w-full py-3 text-base">Checkout</Button>
									</div>
									<div class="mt-6 flex justify-center text-center text-sm text-slate-500">
										<p>
											or
											<button 
												type="button" 
												class="font-medium text-slate-600 hover:text-slate-500 ml-1 cursor-pointer"
												onclick={close}
											>
												Continue Shopping
												<span aria-hidden="true"> &rarr;</span>
											</button>
										</p>
									</div>
								</div>
							{/if}
						</div>
					</div>
				</div>
			</div>
		</div>
	</div>
{/if}
