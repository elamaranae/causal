import { test, expect } from '../helpers/fixtures';

test.describe('Auth API', () => {
  test('register a new user', async ({ apiClient, testEmail, testPassword }) => {
    const res = await apiClient.register(testEmail, testPassword);
    expect(res.status()).toBe(200);
  });

  test('login with registered user', async ({ apiClient, testEmail, testPassword }) => {
    await apiClient.register(testEmail, testPassword);
    const res = await apiClient.login(testEmail, testPassword);
    expect(res.status()).toBe(200);
  });

  test('get current user after login', async ({ authedClient }) => {
    const res = await authedClient.get('/auth/me');
    expect(res.status()).toBe(200);
    const user = await res.json();
    expect(user).toHaveProperty('id');
    expect(user).toHaveProperty('email');
  });

  test('refresh token', async ({ authedClient }) => {
    const res = await authedClient.post('/auth/refresh');
    expect(res.status()).toBe(200);
  });

  test('logout clears session', async ({ authedClient }) => {
    const logoutRes = await authedClient.post('/auth/logout');
    expect(logoutRes.status()).toBe(200);

    const meRes = await authedClient.get('/auth/me');
    expect(meRes.status()).toBeGreaterThanOrEqual(401);
  });

  test('login with wrong password returns 401', async ({ apiClient, testEmail, testPassword }) => {
    await apiClient.register(testEmail, testPassword);
    const res = await apiClient.login(testEmail, 'WrongPassword!');
    expect(res.status()).toBe(401);
  });
});
