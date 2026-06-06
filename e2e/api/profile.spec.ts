import { test, expect } from '../helpers/fixtures';

test.describe('Profile API', () => {
  test('create profile', async ({ authedClient }) => {
    const res = await authedClient.post('/profiles/me', {
      firstName: 'Test',
      lastName: 'User',
      currency: 'USD',
    });
    expect(res.status()).toBe(200);
    const profile = await res.json();
    expect(profile.firstName).toBe('Test');
    expect(profile.lastName).toBe('User');
    expect(profile.currency).toBe('USD');
  });

  test('update profile', async ({ authedClient }) => {
    await authedClient.post('/profiles/me', {
      firstName: 'Test',
      lastName: 'User',
      currency: 'USD',
    });

    const res = await authedClient.patch('/profiles/me', {
      firstName: 'Updated',
      currency: 'EUR',
    });
    expect(res.status()).toBe(200);
    const profile = await res.json();
    expect(profile.firstName).toBe('Updated');
    expect(profile.currency).toBe('EUR');
  });

  test('add address', async ({ authedClient }) => {
    await authedClient.post('/profiles/me', {
      firstName: 'Test',
      lastName: 'User',
      currency: 'USD',
    });

    const res = await authedClient.post('/profiles/me/addresses', {
      label: 'Home',
      line1: '123 Test St',
      city: 'Testville',
      state: 'TS',
      country: 'US',
      pincode: '12345',
      phoneNumber: '555-0100',
    });
    expect(res.status()).toBe(200);
    const address = await res.json();
    expect(address.label).toBe('Home');
    expect(address.line1).toBe('123 Test St');
  });

  test('update address', async ({ authedClient }) => {
    await authedClient.post('/profiles/me', {
      firstName: 'Test',
      lastName: 'User',
      currency: 'USD',
    });

    const addRes = await authedClient.post('/profiles/me/addresses', {
      label: 'Home',
      line1: '123 Test St',
      city: 'Testville',
      state: 'TS',
      country: 'US',
      pincode: '12345',
    });
    const address = await addRes.json();

    const updateRes = await authedClient.patch(`/profiles/me/addresses/${address.id}`, {
      line1: '456 Updated Ave',
    });
    expect(updateRes.status()).toBe(200);
    const updated = await updateRes.json();
    expect(updated.line1).toBe('456 Updated Ave');
  });

  test('delete address', async ({ authedClient }) => {
    await authedClient.post('/profiles/me', {
      firstName: 'Test',
      lastName: 'User',
      currency: 'USD',
    });

    // Create two addresses — first becomes default and can't be deleted
    await authedClient.post('/profiles/me/addresses', {
      label: 'Home',
      line1: '100 Main St',
      city: 'Testville',
      state: 'TS',
      country: 'US',
      pincode: '11111',
    });

    const addRes = await authedClient.post('/profiles/me/addresses', {
      label: 'Temp',
      line1: '789 Delete Rd',
      city: 'Testville',
      state: 'TS',
      country: 'US',
      pincode: '99999',
    });
    const address = await addRes.json();

    const deleteRes = await authedClient.delete(`/profiles/me/addresses/${address.id}`);
    expect(deleteRes.status()).toBe(200);

    const listRes = await authedClient.get('/profiles/me/addresses');
    const addresses = await listRes.json();
    expect(Array.isArray(addresses)).toBe(true);
    const found = (addresses as Array<{ id: number }>).find((a) => a.id === address.id);
    expect(found).toBeUndefined();
  });
});
