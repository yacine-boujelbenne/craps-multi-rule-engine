import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { APP_CONFIG } from '../config/app-config';
import {
  CreateOrganizationRequest,
  Organization,
  OrganizationPage,
  UpdateOrganizationRequest,
} from '../models/commercial.model';
import { toApiProblem } from '../utils/problem';
import { OrganizationListQuery, OrganizationService } from './organization.service';

/** HTTP implementation of {@link OrganizationService}. */
@Injectable()
export class OrganizationApiService implements OrganizationService {
  private readonly http = inject(HttpClient);
  private readonly base = `${APP_CONFIG.apiBaseUrl}/organizations`;

  list(query: OrganizationListQuery = {}): Observable<OrganizationPage> {
    let params = new HttpParams();
    if (query.page !== undefined) {
      params = params.set('page', query.page);
    }
    if (query.size !== undefined) {
      params = params.set('size', query.size);
    }
    if (query.q?.trim()) {
      params = params.set('q', query.q.trim());
    }
    if (query.includeInactive) {
      params = params.set('includeInactive', true);
    }
    if (query.role) {
      params = params.set('role', query.role);
    }
    return this.http
      .get<OrganizationPage>(this.base, { params, withCredentials: true })
      .pipe(catchError(toApiProblem));
  }

  get(id: string): Observable<Organization> {
    return this.http
      .get<Organization>(`${this.base}/${id}`, { withCredentials: true })
      .pipe(catchError(toApiProblem));
  }

  create(input: CreateOrganizationRequest): Observable<Organization> {
    return this.http
      .post<Organization>(this.base, input, { withCredentials: true })
      .pipe(catchError(toApiProblem));
  }

  update(id: string, patch: UpdateOrganizationRequest): Observable<Organization> {
    return this.http
      .patch<Organization>(`${this.base}/${id}`, patch, { withCredentials: true })
      .pipe(catchError(toApiProblem));
  }
}
