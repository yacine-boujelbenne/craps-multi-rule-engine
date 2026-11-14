import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, catchError, map, of, shareReplay, switchMap, tap, throwError } from 'rxjs';
import {
  AccountRole,
  ChangePasswordRequest,
  LoginRequest,
  User,
  isAdmin as userIsAdmin,
} from '../models/account.model';
import { ApiProblemError } from '../models/project.model';
import { AUTH_SERVICE } from './auth.service';

export type AuthStatus = 'unknown' | 'anonymous' | 'authenticated' | 'password-change-required';

/**
 * Signal-based session container. Holds the current account and drives the
 * route guards. The server remains the authority: `me()` is re-read on
 * bootstrap, and every guarded request is independently authorized server-side.
 */
@Injectable({ providedIn: 'root' })
export class AuthStore {
  private readonly service = inject(AUTH_SERVICE);

  private readonly _user = signal<User | null>(null);
  private readonly _status = signal<AuthStatus>('unknown');
  private readonly _loading = signal(false);
  private readonly _error = signal<string | null>(null);

  private load$: Observable<User | null> | null = null;

  readonly user = this._user.asReadonly();
  readonly status = this._status.asReadonly();
  readonly loading = this._loading.asReadonly();
  readonly error = this._error.asReadonly();

  readonly isAuthenticated = computed(() => this._status() === 'authenticated');
  readonly mustChangePassword = computed(() => this._status() === 'password-change-required');
  readonly isAdmin = computed(() => userIsAdmin(this._user()));
  readonly roles = computed<AccountRole[]>(() => this._user()?.roles ?? []);

  hasRole(role: AccountRole): boolean {
    return this.roles().includes(role);
  }

  /** Reads the current account once; resolves `null` for an anonymous session. */
  bootstrap(): Observable<User | null> {
    if (!this.load$) {
      this.load$ = this.service.me().pipe(
        map((user) => {
          this.setUser(user);
          return user;
        }),
        catchError(() => {
          this.setAnonymous();
          return of(null);
        }),
        shareReplay(1),
      );
    }
    return this.load$;
  }

  login(credentials: LoginRequest): Observable<User> {
    this._loading.set(true);
    this._error.set(null);
    return this.service.csrf().pipe(
      switchMap(() => this.service.login(credentials)),
      tap((user) => this.setUser(user)),
      // The server rotates/clears the CSRF token on login; refresh it.
      switchMap((user) => this.service.csrf().pipe(map(() => user))),
      tap(() => {
        this._loading.set(false);
        this.load$ = null;
      }),
      catchError((error: unknown) => {
        this._loading.set(false);
        this._error.set(problemDetail(error) ?? 'Sign-in failed. Please try again.');
        return throwError(() => error);
      }),
    );
  }

  logout(): Observable<void> {
    return this.service.csrf().pipe(
      switchMap(() => this.service.logout()),
      switchMap(() => this.service.csrf()),
      catchError(() => of(undefined)),
      tap(() => this.clear()),
    );
  }

  changePassword(input: ChangePasswordRequest): Observable<void> {
    return this.service.changePassword(input).pipe(
      switchMap(() => this.service.csrf()),
      tap(() => this.clear()),
    );
  }

  /** Clears local session state; the server session is invalidated separately. */
  clear(): void {
    this._user.set(null);
    this._status.set('anonymous');
    this._error.set(null);
    this.load$ = null;
  }

  private setUser(user: User): void {
    this._user.set(user);
    this._status.set(user.mustChangePassword ? 'password-change-required' : 'authenticated');
    this._error.set(null);
  }

  private setAnonymous(): void {
    this._user.set(null);
    this._status.set('anonymous');
  }
}

function problemDetail(error: unknown): string | null {
  return error instanceof ApiProblemError
    ? (error.problem.detail ?? error.problem.title ?? null)
    : null;
}
