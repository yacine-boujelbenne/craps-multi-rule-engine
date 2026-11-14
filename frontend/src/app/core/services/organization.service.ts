import { InjectionToken } from '@angular/core';
import { Observable } from 'rxjs';
import {
  CreateOrganizationRequest,
  Organization,
  OrganizationPage,
  OrganizationRole,
  UpdateOrganizationRequest,
} from '../models/commercial.model';

export interface OrganizationListQuery {
  page?: number;
  size?: number;
  q?: string;
  includeInactive?: boolean;
  role?: OrganizationRole;
}

/**
 * Organization (company) directory (`docs/contracts/commercial-setup-v1.yaml`):
 *
 *   GET    /api/organizations?page&size&q&includeInactive&role
 *   POST   /api/organizations                       (ADMIN)
 *   GET    /api/organizations/{id}
 *   PATCH  /api/organizations/{id}                  (ADMIN, requires expectedVersion)
 *
 * Reads are available to any authenticated read role; every write requires
 * ADMIN, enforced by the server independently of form assignments.
 */
export interface OrganizationService {
  list(query?: OrganizationListQuery): Observable<OrganizationPage>;
  get(id: string): Observable<Organization>;
  create(input: CreateOrganizationRequest): Observable<Organization>;
  update(id: string, patch: UpdateOrganizationRequest): Observable<Organization>;
}

export const ORGANIZATION_SERVICE = new InjectionToken<OrganizationService>('ORGANIZATION_SERVICE');
