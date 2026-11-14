import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { beforeEach, describe, expect, it } from 'vitest';
import { AUTH_SERVICE } from './auth.service';
import { AuthMockService, MOCK_ACCOUNTS_SEED } from './auth-mock.service';
import { AuthStore } from './auth.store';
import { MOCK_LATENCY_MS } from './project-mock.service';
import { ApiProblemError } from '../models/project.model';
import { TEST_ACCOUNTS } from '../testing/mock-accounts';

describe('AuthStore', () => {
  let store: AuthStore;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        { provide: MOCK_LATENCY_MS, useValue: 0 },
        { provide: MOCK_ACCOUNTS_SEED, useValue: TEST_ACCOUNTS },
        { provide: AUTH_SERVICE, useClass: AuthMockService },
      ],
    });
    store = TestBed.inject(AuthStore);
  });

  it('starts unknown and reports anonymous after an empty bootstrap', async () => {
    expect(store.status()).toBe('unknown');
    await firstValueFrom(store.bootstrap());
    expect(store.status()).toBe('anonymous');
    expect(store.user()).toBeNull();
  });

  it('authenticates an admin and exposes the admin role', async () => {
    await firstValueFrom(store.login({ username: 'admin', password: 'Admin-Passw0rd' }));
    expect(store.status()).toBe('authenticated');
    expect(store.isAuthenticated()).toBe(true);
    expect(store.isAdmin()).toBe(true);
    expect(store.roles()).toContain('ADMIN');
  });

  it('flags an account that must change its password', async () => {
    await firstValueFrom(store.login({ username: 'new.user', password: 'Temporary-Pass1' }));
    expect(store.status()).toBe('password-change-required');
    expect(store.mustChangePassword()).toBe(true);
    expect(store.isAuthenticated()).toBe(false);
  });

  it('rejects invalid credentials and keeps the session anonymous', async () => {
    await expect(
      firstValueFrom(store.login({ username: 'admin', password: 'wrong-password' })),
    ).rejects.toBeInstanceOf(ApiProblemError);
    expect(store.isAuthenticated()).toBe(false);
    expect(store.user()).toBeNull();
  });

  it('clears the session on logout', async () => {
    await firstValueFrom(store.login({ username: 'admin', password: 'Admin-Passw0rd' }));
    await firstValueFrom(store.logout());
    expect(store.status()).toBe('anonymous');
    expect(store.user()).toBeNull();
  });

  it('clears the session after a successful password change', async () => {
    await firstValueFrom(store.login({ username: 'new.user', password: 'Temporary-Pass1' }));
    await firstValueFrom(
      store.changePassword({ currentPassword: 'Temporary-Pass1', newPassword: 'Brand-New-Pass1' }),
    );
    expect(store.status()).toBe('anonymous');
    expect(store.user()).toBeNull();
  });
});
