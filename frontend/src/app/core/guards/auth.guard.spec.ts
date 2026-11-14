import { TestBed } from '@angular/core/testing';
import {
  ActivatedRouteSnapshot,
  CanActivateFn,
  RouterStateSnapshot,
  UrlTree,
  provideRouter,
} from '@angular/router';
import { firstValueFrom, isObservable } from 'rxjs';
import { beforeEach, describe, expect, it } from 'vitest';
import { adminGuard, authGuard, guestGuard, passwordChangeGuard } from './auth.guard';
import { AssignmentMockService } from '../services/assignment-mock.service';
import { ASSIGNMENT_SERVICE } from '../services/assignment.service';
import { AuthMockService, MOCK_ACCOUNTS_SEED } from '../services/auth-mock.service';
import { AUTH_SERVICE } from '../services/auth.service';
import { AuthStore } from '../services/auth.store';
import { MOCK_LATENCY_MS } from '../services/project-mock.service';
import { TEST_ACCOUNTS } from '../testing/mock-accounts';

const ROUTE = { queryParamMap: { get: () => null } } as unknown as ActivatedRouteSnapshot;
const STATE = { url: '/dashboard' } as RouterStateSnapshot;

describe('auth guards', () => {
  let auth: AuthStore;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: MOCK_LATENCY_MS, useValue: 0 },
        { provide: MOCK_ACCOUNTS_SEED, useValue: TEST_ACCOUNTS },
        { provide: AUTH_SERVICE, useClass: AuthMockService },
        { provide: ASSIGNMENT_SERVICE, useClass: AssignmentMockService },
      ],
    });
    auth = TestBed.inject(AuthStore);
  });

  function run(fn: CanActivateFn) {
    const result = TestBed.runInInjectionContext(() => fn(ROUTE, STATE));
    return isObservable(result) ? firstValueFrom(result) : Promise.resolve(result);
  }

  it('redirects an anonymous session to the login page', async () => {
    const result = await run(authGuard);
    expect(result).toBeInstanceOf(UrlTree);
    expect(String(result)).toContain('/login');
  });

  it('allows an authenticated admin through authGuard', async () => {
    await firstValueFrom(auth.login({ username: 'admin', password: 'Admin-Passw0rd' }));
    expect(await run(authGuard)).toBe(true);
  });

  it('confines a password-change session to the change-password screen', async () => {
    await firstValueFrom(auth.login({ username: 'new.user', password: 'Temporary-Pass1' }));
    const result = await run(authGuard);
    expect(result).toBeInstanceOf(UrlTree);
    expect(String(result)).toContain('/change-password');
  });

  it('denies a non-admin operator on adminGuard', async () => {
    await firstValueFrom(auth.login({ username: 'operator', password: 'Operator-Passw0rd' }));
    const result = await run(adminGuard);
    expect(result).toBeInstanceOf(UrlTree);
    expect(String(result)).toContain('/dashboard');
  });

  it('sends an authenticated user away from the guest login screen', async () => {
    await firstValueFrom(auth.login({ username: 'admin', password: 'Admin-Passw0rd' }));
    const result = await run(guestGuard);
    expect(result).toBeInstanceOf(UrlTree);
    expect(String(result)).toContain('/dashboard');
  });

  it('blocks the password screen when no change is required', async () => {
    await firstValueFrom(auth.login({ username: 'admin', password: 'Admin-Passw0rd' }));
    const result = await run(passwordChangeGuard);
    expect(result).toBeInstanceOf(UrlTree);
    expect(String(result)).toContain('/dashboard');
  });

  it('allows the password screen when a change is required', async () => {
    await firstValueFrom(auth.login({ username: 'new.user', password: 'Temporary-Pass1' }));
    expect(await run(passwordChangeGuard)).toBe(true);
  });
});
