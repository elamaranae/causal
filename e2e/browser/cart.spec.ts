import { test, expect } from '@playwright/test';
import { registerAndLogin, navigateToInStockProduct } from '../helpers/browser';
import { ApiClient } from '../helpers/api';
import { findInStockProduct } from '../helpers/products';

test.describe('Cart flows', () => {
  test('add product to cart from product detail', async ({ page }) => {
    await registerAndLogin(page);
    const product = await findInStockProduct(new ApiClient(page.request));
    await navigateToInStockProduct(page, product);
    await page.getByRole('button', { name: 'Add to Cart' }).click();

    await expect(page.locator('header span.rounded-full').first()).toBeVisible({ timeout: 5_000 });

    // Open cart drawer and verify
    await page.locator('header button:has(svg)').first().click();
    await expect(page.getByText('Shopping cart')).toBeVisible();
  });

  test('update quantity in cart drawer', async ({ page }) => {
    await registerAndLogin(page);
    const product = await findInStockProduct(new ApiClient(page.request));
    await navigateToInStockProduct(page, product);
    await page.getByRole('button', { name: 'Add to Cart' }).click();
    await expect(page.locator('header span.rounded-full').first()).toBeVisible({ timeout: 5_000 });

    // Open cart drawer and increment
    await page.locator('header button:has(svg)').first().click();
    await expect(page.getByText('Shopping cart')).toBeVisible();

    await page.getByRole('button', { name: '+' }).click();
    await expect(page.locator('.flow-root span.font-medium:has-text("2")')).toBeVisible({
      timeout: 5_000,
    });
  });

  test('remove item from cart via product detail', async ({ page }) => {
    await registerAndLogin(page);
    const product = await findInStockProduct(new ApiClient(page.request));
    await navigateToInStockProduct(page, product);
    await page.getByRole('button', { name: 'Add to Cart' }).click();

    await expect(page.getByRole('button', { name: 'Remove from Cart' })).toBeVisible({
      timeout: 5_000,
    });
    await page.getByRole('button', { name: 'Remove from Cart' }).click();
    await expect(page.getByRole('button', { name: 'Add to Cart' })).toBeVisible({
      timeout: 5_000,
    });
  });
});
