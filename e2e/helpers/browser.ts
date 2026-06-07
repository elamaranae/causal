import { expect } from '@playwright/test';
import type { Page } from '@playwright/test';
import type { InStockProduct } from './products';

export const PASSWORD = 'TestPass123!';

export function uniqueEmail(prefix: string): string {
  return `e2e-${prefix}-${Date.now()}-${Math.random().toString(36).slice(2, 8)}@test.dev`;
}


/** Navigates to a product page and selects the variant with stock. */
export async function navigateToInStockProduct(page: Page, product: InStockProduct) {
  await page.goto(`/products/${product.productId}`);
  // Click variant buttons to select the in-stock SKU
  for (const [, value] of Object.entries(product.variantAttributes)) {
    const btn = page.locator('fieldset button', { hasText: value });
    await btn.click();
  }
  await expect(page.getByRole('button', { name: 'Add to Cart' })).toBeVisible({ timeout: 5_000 });
}

export async function registerAndLogin(page: Page) {
  const email = uniqueEmail('browser');
  await page.goto('/register');
  await page.getByLabel('Email address').fill(email);
  await page.getByLabel('Password', { exact: true }).fill(PASSWORD);
  await page.getByLabel('Confirm Password').fill(PASSWORD);
  await page.getByRole('button', { name: 'Create account' }).click();
  await expect(page).toHaveURL('/', { timeout: 10_000 });
  return email;
}
