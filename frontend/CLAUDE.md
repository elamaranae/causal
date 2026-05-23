# Causal Frontend

SvelteKit 5 SPA (client-side only, `ssr: false`) with Tailwind CSS 4. Static adapter builds a client-side shell.

## Tech Stack

- **Svelte 5** with runes (`$state`, `$derived`, `$effect`)
- **SvelteKit 2** with `@sveltejs/adapter-static`, `ssr: false`
- **Tailwind CSS 4** via `@tailwindcss/vite`
- **TypeScript** strict mode
- **2-space indentation** (not tabs)

## Architecture

### API Layer (`src/lib/api.ts`)

Single file with:
- `urls` object — all backend endpoint URLs, organized by domain (`urls.auth.*`, `urls.products.*`, `urls.cart.*`)
- `apiFetch()` — fetch wrapper that handles JSON content-type, CSRF tokens (read from `csrf_token` cookie), credentials (`include`), and automatic token refresh on 401/403

CSRF token is re-read after token refresh to avoid stale tokens. Token refresh uses a shared promise to deduplicate concurrent refresh attempts.

Circular dependency: `api.ts` imports `auth` (to check `isAuthenticated` for refresh logic), `auth.svelte.ts` imports `apiFetch`/`urls`. This works due to ES module evaluation order.

Base URL defaults to `http://causal-gateway/api`, overridden via `VITE_API_URL` env var.

### Auth (`src/lib/auth.svelte.ts`)

`AuthState` class exported as singleton `auth`. Uses `$state` for `user`, derives `isAuthenticated` from `user !== null`. Persists user to localStorage. Methods: `fetchUser()`, `onLoginSuccess()`, `logout()`.

### Cart (`src/lib/cart.svelte.ts`)

`CartStore` class exported as singleton `cart`. Fully server-backed — no localStorage.

- `load()` — GET `/cart/me` then POST `/products/skus/bulk` to enrich items with product/sku details. Only shows loading spinner on initial load (empty cart), not on refreshes after mutations.
- `addItem(skuId)` — POST, then reloads
- `updateQuantity(cartItemId, quantity)` — PATCH, then reloads. Handles `<= 0` as remove.
- `removeItem(cartItemId)` — DELETE, then reloads
- `clear()` — local-only, used on logout

Cart is loaded in `+layout.svelte` `onMount` if authenticated, and after login/register via `cart.load()`.

### Types (`src/lib/types.ts`)

All API response types. Key types:
- `ProductListing` — listing card data, includes `defaultSku` with `price`
- `ProductShow` — detail page, includes `skus[]` each with `price`, `primaryVariantKey`, `defaultSkuId`
- `Sku` — has `price`, `variantAttributes`, `media` (nested as `{ id, media: MediaItem[] }`)
- `ApiCart` / `ApiCartItem` — raw cart API response (`{ id, skuId, quantity }`)
- `SkuDetail` — enriched SKU from bulk endpoint, includes nested `product` with name/description
- `CartItem` — joined cart item: `{ cartItemId, skuId, quantity, sku: SkuDetail }`

### Data Flow

- **Categories**: Loaded in `+layout.ts` load function, cached by SvelteKit across navigations. Available to all pages via merged `data`.
- **Products**: Loaded in `+page.ts` based on URL search params. `/?category=5&page=2` drives filtering. No category param = trending products.
- **Product detail**: `+page.ts` fetches by ID via `urls.products.show(id)`.
- **After login/register**: `invalidateAll()` forces re-run of all load functions (so categories refresh if they were empty pre-auth).

### URL-Driven Filtering (Home Page)

Category selection and pagination use URL search params instead of component state:
- `goto('/')` — trending (all)
- `goto('/?category=5')` — filtered by category
- `goto('/?category=5&page=2')` — with pagination

The `+page.ts` load function reads params and fetches accordingly. The component is purely presentational.

## File Structure

```
src/
  lib/
    api.ts              # URL definitions + apiFetch wrapper
    auth.svelte.ts      # Auth state (singleton)
    cart.svelte.ts       # Cart state (singleton, server-backed)
    types.ts            # All TypeScript interfaces
    components/
      Button.svelte     # Reusable button (primary/outline variants)
      Input.svelte      # Form input with label
      Logo.svelte       # Brand logo
      CartDrawer.svelte # Slide-over cart panel
    assets/
      favicon.svg
  routes/
    +layout.ts          # Loads categories, sets ssr:false
    +layout.svelte      # Navbar, auth UI, cart button snippet, CartDrawer
    +page.ts            # Loads trending or filtered products from URL params
    +page.svelte        # Home: category sidebar + product grid + pagination
    login/+page.svelte  # Login form
    register/+page.svelte # Register form
    products/[id]/
      +page.ts          # Loads single product
      +page.svelte      # Product detail: gallery, variant selector, add/remove cart
```

## Key Patterns

- **Svelte 5 runes only** — no `$:` reactive statements, no stores (`writable`/`readable`). Uses `$state`, `$derived`, `$derived.by`, `$effect`, `$props`, `$bindable`.
- **Snippets** — `{#snippet}` / `{@render}` used for deduplication (e.g., cart button in layout).
- **`{@const}`** inside `{#each}` for one-time computations per iteration.
- **Class-based singletons** with `$state` for global reactive state (`auth`, `cart`).
- **Auth guard on cart actions** — "Add to Cart" redirects to `/login` if not authenticated. Product detail page shows "Remove from Cart" (red) if the selected SKU is already in cart.
- **Variant selection** — product detail page builds a selection matrix from all SKUs' `variantAttributes`. `primaryVariantKey` determines which variant appears first. Unavailable combinations shown with dashed border + "unavailable" label.
- **Error handling** — `catch (err: unknown)` with `instanceof Error` check, never `err: any`.

## Backend API Endpoints Used

Auth: `POST /auth/login`, `POST /auth/register`, `POST /auth/logout`, `POST /auth/refresh`, `GET /auth/me`
Products: `GET /products/trending`, `GET /products/categories`, `GET /products/filter?categoryId=&page=&size=`, `GET /products/{id}`, `POST /products/skus/bulk`
Cart: `GET /cart/me`, `DELETE /cart/me`, `POST /cart/me/items`, `PATCH /cart/me/items/{id}`, `DELETE /cart/me/items/{id}`

## Svelte MCP Tools

Use the Svelte MCP server for documentation lookup:
1. `list-sections` — discover available doc sections
2. `get-documentation` — fetch relevant sections for the task
3. `svelte-autofixer` — validate Svelte code before finalizing
