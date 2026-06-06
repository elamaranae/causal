import { test, expect } from '@playwright/test';

function uniqueEmail(): string {
  return `e2e-browse-${Date.now()}-${Math.random().toString(36).slice(2, 8)}@test.dev`;
}

const PASSWORD = 'TestPass123!';

async function registerAndLogin(page: import('@playwright/test').Page) {
  const email = uniqueEmail();
  await page.goto('/register');
  await page.getByLabel('Email address').fill(email);
  await page.getByLabel('Password', { exact: true }).fill(PASSWORD);
  await page.getByLabel('Confirm Password').fill(PASSWORD);
  await page.getByRole('button', { name: 'Create account' }).click();
  await expect(page).toHaveURL('/', { timeout: 10_000 });
}

test.describe('Browse products', () => {
  test('home page loads trending products', async ({ page }) => {
    await registerAndLogin(page);
    await expect(page.getByRole('heading', { name: 'Trending' })).toBeVisible();
    const productLinks = page.locator('a[href^="/products/"]');
    await expect(productLinks.first()).toBeVisible({ timeout: 10_000 });
    expect(await productLinks.count()).toBeGreaterThan(0);
  });

  test('click category filters products', async ({ page }) => {
    await registerAndLogin(page);

    // Wait for categories to load in sidebar
    const categoryButtons = page.locator('aside nav button');
    await expect(categoryButtons.nth(1)).toBeVisible({ timeout: 10_000 });

    const firstCategory = categoryButtons.nth(1); // nth(0) is "All"
    const categoryName = await firstCategory.textContent();
    await firstCategory.click();

    await expect(page).toHaveURL(/\?category=\d+/);
    await expect(page.getByRole('heading', { name: categoryName!.trim() })).toBeVisible();
  });

  test('click product card navigates to detail', async ({ page }) => {
    await registerAndLogin(page);
    const firstProduct = page.locator('a[href^="/products/"]').first();
    await expect(firstProduct).toBeVisible({ timeout: 10_000 });
    const productName = await firstProduct.locator('h3').textContent();
    await firstProduct.click();

    await expect(page).toHaveURL(/\/products\/\d+/);
    await expect(page.getByRole('heading', { name: productName!.trim() })).toBeVisible();
  });

  test('product detail shows price and add to cart', async ({ page }) => {
    await registerAndLogin(page);
    const firstProduct = page.locator('a[href^="/products/"]').first();
    await expect(firstProduct).toBeVisible({ timeout: 10_000 });
    await firstProduct.click();
    await expect(page).toHaveURL(/\/products\/\d+/);

    // Price should be visible on the detail page
    await expect(page.locator('article').locator('text=/\\$\\d+\\.\\d{2}/').first()).toBeVisible();

    // Add to Cart or Out of Stock button should be visible
    await expect(
      page.getByRole('button', { name: /Add to Cart|Out of Stock|Unavailable/ })
    ).toBeVisible();
  });
});
