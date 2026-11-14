import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { APP_CONFIG } from '../config/app-config';
import { ChangePasswordRequest, LoginRequest, User } from '../models/account.model';
import { toApiProblem } from '../utils/problem';
import { AuthService } from './auth.service';

/**
 * HTTP implementation of {@link AuthService}. Requests are sent with
 * credentials so the HttpOnly session cookie is included; the Angular HTTP
 * client adds `X-XSRF-TOKEN` (from the `XSRF-TOKEN` cookie) on unsafe methods.
 *
 * Callers must obtain the CSRF cookie via {@link csrf} before login and again
 * after login/logout, because the server rotates/clears it on those events.
 */
@Injectable()
export class AuthApiService implements AuthService {
  private readonly http = inject(HttpClient);
  private readonly base = `${APP_CONFIG.apiBaseUrl}/auth`;

  csrf(): Observable<void> {
    return this.http
      .get<void>(`${this.base}/csrf`, { withCredentials: true })
      .pipe(catchError(toApiProblem));
  }

  login(credentials: LoginRequest): Observable<User> {
    return this.http
      .post<User>(`${this.base}/login`, credentials, { withCredentials: true })
      .pipe(catchError(toApiProblem));
  }

  logout(): Observable<void> {
    return this.http
      .post<void>(`${this.base}/logout`, {}, { withCredentials: true })
      .pipe(catchError(toApiProblem));
  }

  me(): Observable<User> {
    return this.http
      .get<User>(`${this.base}/me`, { withCredentials: true })
      .pipe(catchError(toApiProblem));
  }

  changePassword(input: ChangePasswordRequest): Observable<void> {
    return this.http
      .post<void>(`${this.base}/password`, input, { withCredentials: true })
      .pipe(catchError(toApiProblem));
  }
}
