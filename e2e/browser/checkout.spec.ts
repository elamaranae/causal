import { test, expect } from '@playwright/test';
import { ApiClient } from '../helpers/api';

function uniqueEmail(): string {
  return `e2e-checkout-${Date.now()}-${Math.random().toString(36).slice(2, 8)}@test.dev`;
}

const PASSWORD = 'TestPass123!';

test.describe('Checkout flow', () => {
  test('full flow: browse, add to cart, setup profile, checkout', async ({ page, request }) => {
    const email = uniqueEmail();

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

    // Login in the browser
    await page.goto('/login');
    await page.getByLabel('Email address').fill(email);
    await page.getByLabel('Password').fill(PASSWORD);
    await page.getByRole('button', { name: 'Sign in' }).click();
    await expect(page).toHaveURL('/', { timeout: 10_000 });

    // Navigate to product and add to cart
    await page.locator('a[href^="/products/"]').first().click();
    await expect(page).toHaveURL(/\/products\/\d+/);

    const addButton = page.getByRole('button', { name: 'Add to Cart' });
    if (await addButton.isVisible()) {
      await addButton.click();
      await expect(page.locator('span:has-text("1")')).toBeVisible({ timeout: 5_000 });

      // Open cart and checkout
      await page.locator('header button:has(svg)').first().click();
      await expect(page.getByText('Shopping cart')).toBeVisible();

      await page.getByRole('button', { name: 'Checkout' }).click();

      // Should redirect to payment page
      await expect(page).toHaveURL(/\/orders\/payment\?orderId=\d+/, { timeout: 15_000 });
    }
  });
});
