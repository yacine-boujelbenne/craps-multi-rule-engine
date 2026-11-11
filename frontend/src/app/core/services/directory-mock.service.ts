import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { delay } from 'rxjs/operators';
import { MOCK_COUNTRIES, MOCK_ORGANIZATIONS, MOCK_PORTS } from '../data/mock-directory';
import { Country, Organization, OrganizationRole, Port } from '../models/directory.model';
import { DirectoryService } from './directory.service';

const LATENCY_MS = 160;

/**
 * Fixture-backed {@link DirectoryService}. Stands in until real master-data
 * endpoints exist; returns only fictional organizations and ports.
 */
@Injectable()
export class DirectoryMockService implements DirectoryService {
  organizations(role?: OrganizationRole): Observable<Organization[]> {
    const items = MOCK_ORGANIZATIONS.filter(
      (organization) => organization.active && (!role || organization.roles.includes(role)),
    ).map((organization) => structuredClone(organization));
    return of(items).pipe(delay(LATENCY_MS));
  }

  ports(): Observable<Port[]> {
    const items = MOCK_PORTS.filter((port) => port.active).map((port) => structuredClone(port));
    return of(items).pipe(delay(LATENCY_MS));
  }

  countries(): Observable<Country[]> {
    return of(MOCK_COUNTRIES.map((country) => structuredClone(country))).pipe(delay(LATENCY_MS));
  }
}
