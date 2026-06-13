import { test, expect } from '@playwright/test';
import { ApiClient } from '../helpers/api';
import { uniqueEmail, PASSWORD, navigateToInStockProduct } from '../helpers/browser';
import { createAdminClient, createTestProduct } from '../helpers/backoffice';

test.describe('Checkout flow', () => {
  test('full flow: browse, add to cart, setup profile, checkout', async ({ page, request }) => {
    const email = uniqueEmail('checkout');

    // Create a product via backoffice
    const { client: admin, dispose } = await createAdminClient();
    const { productId } = await createTestProduct(admin);
    await dispose();

    // Setup profile & address via API (faster than UI)
    const api = new ApiClient(request);
    await api.register(email, PASSWORD);
    await api.post('/profiles/me', {
      firstName: 'Test',
      lastName: 'Buyer',
      currency: 'USD',
    });
    await api.post('/profiles/me/addresses', {
      label: 'Home',
      line1: '100 Commerce Blvd',
      city: 'Shoptown',
      state: 'CA',
      country: 'US',
      pincode: '90210',
      phoneNumber: '555-0123',
    });

    const product = { productId, variantAttributes: { color: 'Red' } };

    // Login in the browser
    await page.goto('/login');
    await page.getByLabel('Email address').fill(email);
    await page.getByLabel('Password').fill(PASSWORD);
    await page.getByRole('button', { name: 'Sign in' }).click();
    await expect(page).toHaveURL('/', { timeout: 10_000 });

    // Navigate to the in-stock product and select the right variant
    await navigateToInStockProduct(page, product);
    await page.getByRole('button', { name: 'Add to Cart' }).click();
    await expect(page.locator('header span.rounded-full').first()).toBeVisible({ timeout: 5_000 });

    // Open cart and checkout
    await page.locator('header button:has(svg)').first().click();
    await expect(page.getByText('Shopping cart')).toBeVisible();

    await page.getByRole('button', { name: 'Checkout' }).click();
    await expect(page).toHaveURL(/\/orders\/payment\?orderId=\d+/, { timeout: 15_000 });
  });
});
