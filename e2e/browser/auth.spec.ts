import { test, expect } from '@playwright/test';
import { uniqueEmail, PASSWORD, registerAndLogin } from '../helpers/browser';

test.describe('Auth flows', () => {
  test('register and redirect to home', async ({ page }) => {
    await registerAndLogin(page);
    await expect(page.getByRole('button', { name: 'Logout' })).toBeVisible();
  });

  test('login and redirect to home', async ({ page }) => {
    const email = await registerAndLogin(page);

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
    await registerAndLogin(page);
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
