import { Inject, Injectable, InjectionToken, Optional } from '@angular/core';
import { Observable, of, throwError } from 'rxjs';
import { delay } from 'rxjs/operators';
import {
  ChangePasswordRequest,
  LoginRequest,
  User,
  normalizeUsername,
} from '../models/account.model';
import { ApiProblemError } from '../models/project.model';
import { AuthService } from './auth.service';
import { DEFAULT_MOCK_LATENCY_MS, MOCK_LATENCY_MS } from './project-mock.service';

export interface MockAccount {
  user: User;
  password: string;
}

/**
 * The only account seeded by default. Every other account is created through
 * Administration → Accounts, exactly as on the real backend (which ships a
 * single bootstrap administrator). Tests inject their own set via
 * {@link MOCK_ACCOUNTS_SEED}.
 */
export const MOCK_ACCOUNTS: MockAccount[] = [
  {
    user: {
      id: '00000000-0000-4000-8000-000000000001',
      username: 'admin',
      displayName: 'Administrator',
      active: true,
      roles: ['ADMIN'],
      mustChangePassword: false,
      version: 0,
    },
    password: 'Admin-Passw0rd',
  },
];

/** Overridable account seed for the mock auth/account services (tests). */
export const MOCK_ACCOUNTS_SEED = new InjectionToken<MockAccount[]>('MOCK_ACCOUNTS_SEED', {
  factory: () => MOCK_ACCOUNTS,
});

/**
 * In-memory {@link AuthService} for offline development and tests. Credentials
 * are fictional and never persisted; the session lives only in this instance.
 * Enable via `APP_CONFIG.useMockApi = true`.
 */
@Injectable()
export class AuthMockService implements AuthService {
  private accounts: MockAccount[];
  private currentUsername: string | null = null;
  private readonly latency: number;

  constructor(
    @Optional() @Inject(MOCK_LATENCY_MS) latency?: number,
    @Inject(MOCK_ACCOUNTS_SEED) seed: MockAccount[] = MOCK_ACCOUNTS,
  ) {
    this.latency = latency ?? DEFAULT_MOCK_LATENCY_MS;
    this.accounts = (seed ?? MOCK_ACCOUNTS).map((account) => structuredClone(account));
  }

  csrf(): Observable<void> {
    return of(undefined).pipe(delay(this.latency));
  }

  login(credentials: LoginRequest): Observable<User> {
    const username = normalizeUsername(credentials.username);
    const account = this.accounts.find((item) => item.user.username === username);
    if (!account || account.password !== credentials.password || !account.user.active) {
      return this.fail(401, 'Invalid username or password.');
    }
    this.currentUsername = username;
    return of(structuredClone(account.user)).pipe(delay(this.latency));
  }

  logout(): Observable<void> {
    this.currentUsername = null;
    return of(undefined).pipe(delay(this.latency));
  }

  me(): Observable<User> {
    const account = this.currentAccount();
    if (!account) {
      return this.fail(401, 'Authentication required.');
    }
    return of(structuredClone(account.user)).pipe(delay(this.latency));
  }

  changePassword(input: ChangePasswordRequest): Observable<void> {
    const account = this.currentAccount();
    if (!account) {
      return this.fail(401, 'Authentication required.');
    }
    if (account.password !== input.currentPassword) {
      return this.fail(401, 'Current password is incorrect.');
    }
    if (input.newPassword === account.password) {
      return this.fail(
        400,
        'Choose a password different from the current password.',
        'newPassword',
      );
    }
    account.password = input.newPassword;
    account.user = { ...account.user, mustChangePassword: false };
    this.currentUsername = null;
    return of(undefined).pipe(delay(this.latency));
  }

  private currentAccount(): MockAccount | undefined {
    return this.accounts.find((item) => item.user.username === this.currentUsername);
  }

  private fail(status: number, detail: string, field?: string): Observable<never> {
    const problem = {
      type: 'about:blank',
      title: status === 401 ? 'Authentication required' : 'Invalid request',
      status,
      detail,
      ...(field ? { errors: [{ field, code: 'invalid', message: detail }] } : {}),
    };
    return throwError(() => new ApiProblemError(status, problem)).pipe(delay(this.latency));
  }
}
