import { test, expect } from '@playwright/test';

function uniqueEmail(): string {
  return `e2e-cart-${Date.now()}-${Math.random().toString(36).slice(2, 8)}@test.dev`;
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

test.describe('Cart flows', () => {
  test('add product to cart from product detail', async ({ page }) => {
    await registerAndLogin(page);

    // Navigate to a product
    await page.locator('a[href^="/products/"]').first().click();
    await expect(page).toHaveURL(/\/products\/\d+/);

    // Click Add to Cart
    const addButton = page.getByRole('button', { name: 'Add to Cart' });
    if (await addButton.isVisible()) {
      await addButton.click();

      // Cart badge should show
      await expect(page.locator('span:has-text("1")')).toBeVisible({ timeout: 5_000 });

      // Open cart drawer
      await page.locator('header button:has(svg)').first().click();
      await expect(page.getByText('Shopping cart')).toBeVisible();
    }
  });

  test('update quantity in cart drawer', async ({ page }) => {
    await registerAndLogin(page);

    // Add an item
    await page.locator('a[href^="/products/"]').first().click();
    const addButton = page.getByRole('button', { name: 'Add to Cart' });
    if (await addButton.isVisible()) {
      await addButton.click();
      await expect(page.locator('span:has-text("1")')).toBeVisible({ timeout: 5_000 });

      // Open cart drawer and increment
      await page.locator('header button:has(svg)').first().click();
      await expect(page.getByText('Shopping cart')).toBeVisible();

      await page.getByRole('button', { name: '+' }).click();
      await expect(page.locator('span:has-text("2")')).toBeVisible({ timeout: 5_000 });
    }
  });

  test('remove item from cart via product detail', async ({ page }) => {
    await registerAndLogin(page);

    await page.locator('a[href^="/products/"]').first().click();
    const addButton = page.getByRole('button', { name: 'Add to Cart' });
    if (await addButton.isVisible()) {
      await addButton.click();
      await expect(page.getByRole('button', { name: 'Remove from Cart' })).toBeVisible({
        timeout: 5_000,
      });
      await page.getByRole('button', { name: 'Remove from Cart' }).click();
      await expect(page.getByRole('button', { name: 'Add to Cart' })).toBeVisible({
        timeout: 5_000,
      });
    }
  });
});
