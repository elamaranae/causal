import { test, expect } from '@playwright/test';

function uniqueEmail(): string {
  return `e2e-auth-${Date.now()}-${Math.random().toString(36).slice(2, 8)}@test.dev`;
}

const PASSWORD = 'TestPass123!';

test.describe('Auth flows', () => {
  test('register and redirect to home', async ({ page }) => {
    const email = uniqueEmail();
    await page.goto('/register');

    await page.getByLabel('Email address').fill(email);
    await page.getByLabel('Password', { exact: true }).fill(PASSWORD);
    await page.getByLabel('Confirm Password').fill(PASSWORD);
    await page.getByRole('button', { name: 'Create account' }).click();

    await expect(page).toHaveURL('/', { timeout: 10_000 });
    await expect(page.getByRole('button', { name: 'Logout' })).toBeVisible();
  });

  test('login and redirect to home', async ({ page }) => {
    const email = uniqueEmail();

    // Register first
    await page.goto('/register');
    await page.getByLabel('Email address').fill(email);
    await page.getByLabel('Password', { exact: true }).fill(PASSWORD);
    await page.getByLabel('Confirm Password').fill(PASSWORD);
    await page.getByRole('button', { name: 'Create account' }).click();
    await expect(page).toHaveURL('/', { timeout: 10_000 });

    // Logout
    await page.getByRole('button', { name: 'Logout' }).click();
    await expect(page.getByRole('link', { name: 'Log in' })).toBeVisible();

    // Login
    await page.goto('/login');
    await page.getByLabel('Email address').fill(email);
    await page.getByLabel('Password').fill(PASSWORD);
    await page.getByRole('button', { name: 'Sign in' }).click();

    await expect(page).toHaveURL('/', { timeout: 10_000 });
    await expect(page.getByRole('button', { name: 'Logout' })).toBeVisible();
  });

  test('logout shows login link', async ({ page }) => {
    const email = uniqueEmail();
    await page.goto('/register');
    await page.getByLabel('Email address').fill(email);
    await page.getByLabel('Password', { exact: true }).fill(PASSWORD);
    await page.getByLabel('Confirm Password').fill(PASSWORD);
    await page.getByRole('button', { name: 'Create account' }).click();
    await expect(page).toHaveURL('/', { timeout: 10_000 });

    await page.getByRole('button', { name: 'Logout' }).click();
    await expect(page.getByRole('link', { name: 'Log in' })).toBeVisible();
  });

  test('invalid login shows error', async ({ page }) => {
    await page.goto('/login');
    await page.getByLabel('Email address').fill('nonexistent@test.dev');
    await page.getByLabel('Password').fill('WrongPassword!');
    await page.getByRole('button', { name: 'Sign in' }).click();

    await expect(page.locator('.text-red-600')).toBeVisible();
  });
});
