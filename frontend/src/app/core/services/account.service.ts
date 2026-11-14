import { InjectionToken } from '@angular/core';
import { Observable } from 'rxjs';
import {
  CreateUserRequest,
  ResetPasswordRequest,
  UpdateUserRequest,
  User,
  UserPage,
} from '../models/account.model';

/**
 * Administrator account management (`docs/contracts/accounts-access-v1.yaml`):
 *
 *   POST   /api/admin/users
 *   GET    /api/admin/users?page&size
 *   GET    /api/admin/users/{id}
 *   PATCH  /api/admin/users/{id}         (presence-aware, requires expectedVersion)
 *   POST   /api/admin/users/{id}/password (temporary reset, requires expectedVersion)
 *
 * All operations require the ADMIN role; the server enforces this independently.
 */
export interface AccountService {
  list(page?: number, size?: number): Observable<UserPage>;
  get(id: string): Observable<User>;
  create(input: CreateUserRequest): Observable<User>;
  update(id: string, patch: UpdateUserRequest): Observable<User>;
  resetPassword(id: string, input: ResetPasswordRequest): Observable<User>;
}

export const ACCOUNT_SERVICE = new InjectionToken<AccountService>('ACCOUNT_SERVICE');
