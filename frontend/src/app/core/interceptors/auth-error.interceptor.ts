import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { Injector, inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthStore } from '../services/auth.store';

/**
 * Clears local session state and returns to the login screen when the server
 * rejects an authenticated request with 401 (expired or revoked session).
 * {@link AuthStore} is resolved lazily to avoid a HttpClient construction cycle.
 */
export const authErrorInterceptor: HttpInterceptorFn = (request, next) => {
  const injector = inject(Injector);
  const router = inject(Router);
  return next(request).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse && error.status === 401) {
        const auth = injector.get(AuthStore);
        const hadSession = auth.isAuthenticated() || auth.mustChangePassword();
        auth.clear();
        if (hadSession && !router.url.startsWith('/login')) {
          void router.navigate(['/login'], { queryParams: { returnUrl: router.url } });
        }
      }
      return throwError(() => error);
    }),
  );
};
