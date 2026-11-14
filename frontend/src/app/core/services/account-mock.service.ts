import { Inject, Injectable, Optional } from '@angular/core';
import { Observable, of, throwError } from 'rxjs';
import { delay } from 'rxjs/operators';
import {
  AccountRole,
  CreateUserRequest,
  ResetPasswordRequest,
  UpdateUserRequest,
  User,
  UserPage,
  normalizeUsername,
} from '../models/account.model';
import { ApiProblemError } from '../models/project.model';
import { AccountService } from './account.service';
import { MOCK_ACCOUNTS, MOCK_ACCOUNTS_SEED } from './auth-mock.service';
import { DEFAULT_MOCK_LATENCY_MS, MOCK_LATENCY_MS } from './project-mock.service';

/**
 * In-memory {@link AccountService} for offline development. Mirrors the
 * contract's versioning, duplicate-username and last-administrator rules with
 * fictional accounts only.
 */
@Injectable()
export class AccountMockService implements AccountService {
  private users: User[];
  private readonly latency: number;

  constructor(
    @Optional() @Inject(MOCK_LATENCY_MS) latency?: number,
    @Inject(MOCK_ACCOUNTS_SEED) seed: { user: User }[] = MOCK_ACCOUNTS,
  ) {
    this.latency = latency ?? DEFAULT_MOCK_LATENCY_MS;
    this.users = (seed ?? MOCK_ACCOUNTS).map((account) => structuredClone(account.user));
  }

  list(page = 0, size = 20): Observable<UserPage> {
    const ordered = [...this.users].sort(
      (a, b) => a.username.localeCompare(b.username) || a.id.localeCompare(b.id),
    );
    const start = page * size;
    const result: UserPage = {
      items: ordered.slice(start, start + size).map(clone),
      page,
      size,
      totalElements: ordered.length,
      totalPages: ordered.length === 0 ? 0 : Math.ceil(ordered.length / size),
    };
    return of(result).pipe(delay(this.latency));
  }

  get(id: string): Observable<User> {
    const user = this.users.find((item) => item.id === id);
    return user ? of(clone(user)).pipe(delay(this.latency)) : this.fail(404, 'Account not found.');
  }

  create(input: CreateUserRequest): Observable<User> {
    const username = normalizeUsername(input.username);
    if (this.users.some((item) => item.username === username)) {
      return this.fail(409, 'Username already exists.', 'username');
    }
    const user: User = {
      id: generateId(),
      username,
      displayName: input.displayName,
      active: true,
      roles: [...input.roles],
      mustChangePassword: true,
      version: 0,
    };
    this.users = [...this.users, user];
    return of(clone(user)).pipe(delay(this.latency));
  }

  update(id: string, patch: UpdateUserRequest): Observable<User> {
    const current = this.users.find((item) => item.id === id);
    if (!current) {
      return this.fail(404, 'Account not found.');
    }
    if (current.version !== patch.expectedVersion) {
      return this.fail(409, 'Account changed; reload before updating.', 'expectedVersion');
    }
    const username =
      patch.username !== undefined ? normalizeUsername(patch.username) : current.username;
    if (this.users.some((item) => item.id !== id && item.username === username)) {
      return this.fail(409, 'Username already exists.', 'username');
    }
    const active = patch.active ?? current.active;
    const roles = (patch.roles ?? current.roles) as AccountRole[];
    const next: User = {
      ...current,
      username,
      displayName: patch.displayName ?? current.displayName,
      active,
      roles,
    };
    const unchanged =
      next.username === current.username &&
      next.displayName === current.displayName &&
      next.active === current.active &&
      sameRoles(next.roles, current.roles);
    if (unchanged) {
      return of(clone(current)).pipe(delay(this.latency));
    }
    if (
      current.roles.includes('ADMIN') &&
      current.active &&
      (!active || !roles.includes('ADMIN')) &&
      this.activeAdmins() <= 1
    ) {
      return this.fail(409, 'The last active administrator must be retained.', 'roles');
    }
    next.version = current.version + 1;
    this.users = this.users.map((item) => (item.id === id ? next : item));
    return of(clone(next)).pipe(delay(this.latency));
  }

  resetPassword(id: string, input: ResetPasswordRequest): Observable<User> {
    const current = this.users.find((item) => item.id === id);
    if (!current) {
      return this.fail(404, 'Account not found.');
    }
    if (current.version !== input.expectedVersion) {
      return this.fail(409, 'Account changed; reload before updating.', 'expectedVersion');
    }
    const next: User = {
      ...current,
      mustChangePassword: true,
      version: current.version + 1,
    };
    this.users = this.users.map((item) => (item.id === id ? next : item));
    return of(clone(next)).pipe(delay(this.latency));
  }

  private activeAdmins(): number {
    return this.users.filter((user) => user.active && user.roles.includes('ADMIN')).length;
  }

  private fail(status: number, detail: string, field?: string): Observable<never> {
    const problem = {
      type: 'about:blank',
      title: status === 404 ? 'Not found' : status === 409 ? 'Conflict' : 'Invalid request',
      status,
      detail,
      ...(field ? { errors: [{ field, code: 'invalid', message: detail }] } : {}),
    };
    return throwError(() => new ApiProblemError(status, problem)).pipe(delay(this.latency));
  }
}

function sameRoles(a: AccountRole[], b: AccountRole[]): boolean {
  return a.length === b.length && a.every((role) => b.includes(role));
}

function clone<T>(value: T): T {
  return structuredClone(value);
}

function generateId(): string {
  if (typeof crypto !== 'undefined' && 'randomUUID' in crypto) {
    return crypto.randomUUID();
  }
  return `id-${Math.random().toString(36).slice(2)}-${Date.now()}`;
}
