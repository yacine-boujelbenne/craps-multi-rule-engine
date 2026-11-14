import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map, of, switchMap } from 'rxjs';
import { AuthStore } from '../services/auth.store';
import { ASSIGNMENT_SERVICE } from '../services/assignment.service';

/**
 * Route guards backed by {@link AuthStore}. The server remains the authority;
 * these only shape navigation and hide controls. A session that still requires
 * a password change is confined to the change-password screen.
 */
export const authGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthStore);
  const router = inject(Router);
  return auth.bootstrap().pipe(
    map((user) => {
      if (!user) {
        return router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
      }
      if (user.mustChangePassword) {
        return router.createUrlTree(['/change-password']);
      }
      return true;
    }),
  );
};

/** Allows only anonymous sessions; redirects authenticated users onward. */
export const guestGuard: CanActivateFn = (route, _state) => {
  const auth = inject(AuthStore);
  const router = inject(Router);
  return auth.bootstrap().pipe(
    map((user) => {
      if (!user) {
        return true;
      }
      if (user.mustChangePassword) {
        return router.createUrlTree(['/change-password']);
      }
      const returnUrl = route.queryParamMap.get('returnUrl');
      return router.createUrlTree([
        returnUrl && returnUrl.startsWith('/') ? returnUrl : '/dashboard',
      ]);
    }),
  );
};

/** The password-change screen is reachable when authenticated (forced or voluntary). */
export const passwordChangeGuard: CanActivateFn = () => {
  const auth = inject(AuthStore);
  const router = inject(Router);
  return auth.bootstrap().pipe(
    map((user) => {
      if (!user) {
        return router.createUrlTree(['/login']);
      }
      return user.mustChangePassword ? true : router.createUrlTree(['/dashboard']);
    }),
  );
};

/** Requires the ADMIN role. */
export const adminGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthStore);
  const router = inject(Router);
  return auth.bootstrap().pipe(
    map((user) => {
      if (!user) {
        return router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
      }
      if (user.mustChangePassword) {
        return router.createUrlTree(['/change-password']);
      }
      return user.roles.includes('ADMIN') ? true : router.createUrlTree(['/dashboard']);
    }),
  );
};

/**
 * Allows editing a project's setup only when the current user may edit
 * PROJECT_SETUP (admins always may). Reads the effective permission set from
 * the server; falls back to the project workspace when denied.
 */
export const projectSetupGuard: CanActivateFn = (route) => {
  const auth = inject(AuthStore);
  const assignments = inject(ASSIGNMENT_SERVICE);
  const router = inject(Router);
  const id = route.paramMap.get('id') ?? '';
  return auth.bootstrap().pipe(
    switchMap((user) => {
      if (!user) {
        return of(router.createUrlTree(['/login'], { queryParams: { returnUrl: route.url } }));
      }
      if (user.mustChangePassword) {
        return of(router.createUrlTree(['/change-password']));
      }
      if (user.roles.includes('ADMIN')) {
        return of(true);
      }
      return assignments
        .permissions(id)
        .pipe(
          map((permissions) =>
            permissions.editableForms.includes('PROJECT_SETUP')
              ? true
              : router.createUrlTree(['/projects', id]),
          ),
        );
    }),
  );
};
