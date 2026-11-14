import { InjectionToken } from '@angular/core';
import { Observable } from 'rxjs';
import { ChangePasswordRequest, LoginRequest, User } from '../models/account.model';

/**
 * Session authentication against the local-accounts contract
 * (`docs/contracts/accounts-access-v1.yaml`):
 *
 *   GET  /api/auth/csrf      materialize the XSRF-TOKEN cookie
 *   POST /api/auth/login     authenticate; rotate + persist the session
 *   POST /api/auth/logout    invalidate the session and clear cookies
 *   GET  /api/auth/me        current active account and database roles
 *   POST /api/auth/password  replace the password; invalidate old sessions
 *
 * Credentials are never persisted client-side; the server session travels in an
 * HttpOnly `JSESSIONID` cookie and CSRF is handled by the Angular HTTP client.
 */
export interface AuthService {
  /** Obtain/refresh the XSRF-TOKEN cookie; call before login and after login/logout. */
  csrf(): Observable<void>;
  login(credentials: LoginRequest): Observable<User>;
  logout(): Observable<void>;
  me(): Observable<User>;
  changePassword(input: ChangePasswordRequest): Observable<void>;
}

export const AUTH_SERVICE = new InjectionToken<AuthService>('AUTH_SERVICE');
