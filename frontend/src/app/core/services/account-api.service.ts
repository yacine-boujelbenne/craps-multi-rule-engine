import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { APP_CONFIG } from '../config/app-config';
import {
  CreateUserRequest,
  ResetPasswordRequest,
  UpdateUserRequest,
  User,
  UserPage,
} from '../models/account.model';
import { toApiProblem } from '../utils/problem';
import { AccountService } from './account.service';

/** HTTP implementation of {@link AccountService} (ADMIN-only endpoints). */
@Injectable()
export class AccountApiService implements AccountService {
  private readonly http = inject(HttpClient);
  private readonly base = `${APP_CONFIG.apiBaseUrl}/admin/users`;

  list(page = 0, size = 20): Observable<UserPage> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http
      .get<UserPage>(this.base, { params, withCredentials: true })
      .pipe(catchError(toApiProblem));
  }

  get(id: string): Observable<User> {
    return this.http
      .get<User>(`${this.base}/${id}`, { withCredentials: true })
      .pipe(catchError(toApiProblem));
  }

  create(input: CreateUserRequest): Observable<User> {
    return this.http
      .post<User>(this.base, input, { withCredentials: true })
      .pipe(catchError(toApiProblem));
  }

  update(id: string, patch: UpdateUserRequest): Observable<User> {
    return this.http
      .patch<User>(`${this.base}/${id}`, patch, { withCredentials: true })
      .pipe(catchError(toApiProblem));
  }

  resetPassword(id: string, input: ResetPasswordRequest): Observable<User> {
    return this.http
      .post<User>(`${this.base}/${id}/password`, input, { withCredentials: true })
      .pipe(catchError(toApiProblem));
  }
}
