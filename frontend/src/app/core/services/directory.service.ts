import { InjectionToken } from '@angular/core';
import { Observable } from 'rxjs';
import { Country, Organization, OrganizationRole, Port } from '../models/directory.model';

/**
 * Master-data lookups used to resolve party/port ids and populate selectors.
 *
 * No directory endpoints exist in the frozen Phase 1 contract; the only
 * implementation is the fictional-fixture mock. Provide a real HTTP
 * implementation here once Agent 2 exposes lookup endpoints.
 */
export interface DirectoryService {
  organizations(role?: OrganizationRole): Observable<Organization[]>;
  ports(): Observable<Port[]>;
  countries(): Observable<Country[]>;
}

export const DIRECTORY_SERVICE = new InjectionToken<DirectoryService>('DIRECTORY_SERVICE');
