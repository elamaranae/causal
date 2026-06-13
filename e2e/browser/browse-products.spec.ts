import { test, expect } from '@playwright/test';
import { registerAndLogin } from '../helpers/browser';
import { createAdminClient, createTestProduct } from '../helpers/backoffice';

test.describe('Browse products', () => {
  test('home page loads trending products', async ({ page }) => {
    const { client: admin, dispose } = await createAdminClient();
    await createTestProduct(admin);
    await dispose();

    await registerAndLogin(page);
    await expect(page.getByRole('heading', { name: 'Trending' })).toBeVisible();
    const productLinks = page.locator('a[href^="/products/"]');
    await expect(productLinks.first()).toBeVisible({ timeout: 10_000 });
    expect(await productLinks.count()).toBeGreaterThan(0);
  });

  test('click category filters products', async ({ page }) => {
    const { client: admin, dispose } = await createAdminClient();
    await createTestProduct(admin);
    await dispose();

    await registerAndLogin(page);

    const categoryButtons = page.locator('aside nav button');
    await expect(categoryButtons.nth(1)).toBeVisible({ timeout: 10_000 });

    const firstCategory = categoryButtons.nth(1); // nth(0) is "All"
    const categoryName = await firstCategory.textContent();
    await firstCategory.click();

    await expect(page).toHaveURL(/\?category=\d+/);
    await expect(page.getByRole('heading', { name: categoryName!.trim() })).toBeVisible();
  });

  test('click product card navigates to detail', async ({ page }) => {
    const { client: admin, dispose } = await createAdminClient();
    await createTestProduct(admin);
    await dispose();

    await registerAndLogin(page);
    const firstProduct = page.locator('a[href^="/products/"]').first();
    await expect(firstProduct).toBeVisible({ timeout: 10_000 });
    const productName = await firstProduct.locator('h3').textContent();
    await firstProduct.click();

    await expect(page).toHaveURL(/\/products\/\d+/);
    await expect(page.getByRole('heading', { name: productName!.trim() })).toBeVisible();
  });

  test('product detail shows price and add to cart', async ({ page }) => {
    const { client: admin, dispose } = await createAdminClient();
    await createTestProduct(admin);
    await dispose();

    await registerAndLogin(page);
    const firstProduct = page.locator('a[href^="/products/"]').first();
    await expect(firstProduct).toBeVisible({ timeout: 10_000 });
    await firstProduct.click();
    await expect(page).toHaveURL(/\/products\/\d+/);

    await expect(page.locator('article').locator('text=/\\$\\d+\\.\\d{2}/').first()).toBeVisible();
    await expect(
      page.getByRole('button', { name: /Add to Cart|Out of Stock|Unavailable/ }),
    ).toBeVisible();
  });
});
