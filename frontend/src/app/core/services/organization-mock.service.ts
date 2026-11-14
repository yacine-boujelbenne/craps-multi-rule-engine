import { Inject, Injectable, InjectionToken, Optional } from '@angular/core';
import { Observable, of, throwError } from 'rxjs';
import { delay } from 'rxjs/operators';
import {
  CreateOrganizationRequest,
  Organization,
  OrganizationPage,
  UpdateOrganizationRequest,
} from '../models/commercial.model';
import { ApiProblemError } from '../models/project.model';
import { OrganizationListQuery, OrganizationService } from './organization.service';
import { DEFAULT_MOCK_LATENCY_MS, MOCK_LATENCY_MS } from './project-mock.service';

/** Fictional organizations for offline development; empty by default. */
export const MOCK_ORGANIZATIONS_SEED = new InjectionToken<Organization[]>(
  'MOCK_ORGANIZATIONS_SEED',
  { factory: () => [] },
);

/**
 * In-memory {@link OrganizationService} mirroring the contract: zero-based
 * pagination, literal name prefix filtering, active-only by default,
 * `includeInactive` opt-in and versioned PATCH.
 */
@Injectable()
export class OrganizationMockService implements OrganizationService {
  private organizations: Organization[];
  private readonly latency: number;

  constructor(
    @Optional() @Inject(MOCK_LATENCY_MS) latency?: number,
    @Inject(MOCK_ORGANIZATIONS_SEED) seed: Organization[] = [],
  ) {
    this.latency = latency ?? DEFAULT_MOCK_LATENCY_MS;
    this.organizations = (seed ?? []).map(clone);
  }

  list(query: OrganizationListQuery = {}): Observable<OrganizationPage> {
    const page = Math.max(0, query.page ?? 0);
    const size = Math.min(100, Math.max(1, query.size ?? 20));

    let items = [...this.organizations];
    if (!query.includeInactive) {
      items = items.filter((organization) => organization.active);
    }
    if (query.role) {
      items = items.filter((organization) => organization.roles.includes(query.role!));
    }
    const q = query.q?.trim().toLowerCase();
    if (q) {
      items = items.filter((organization) => organization.legalName.toLowerCase().startsWith(q));
    }

    items.sort((a, b) => a.legalName.localeCompare(b.legalName) || a.id.localeCompare(b.id));

    const totalElements = items.length;
    const totalPages = totalElements === 0 ? 0 : Math.ceil(totalElements / size);
    const start = page * size;
    const result: OrganizationPage = {
      items: items.slice(start, start + size).map(clone),
      page,
      size,
      totalElements,
      totalPages,
    };
    return of(result).pipe(delay(this.latency));
  }

  get(id: string): Observable<Organization> {
    const organization = this.organizations.find((item) => item.id === id);
    return organization
      ? of(clone(organization)).pipe(delay(this.latency))
      : this.fail(404, 'Organization not found.');
  }

  create(input: CreateOrganizationRequest): Observable<Organization> {
    if (input.roles.length === 0) {
      return this.fail(400, 'At least one role is required.', 'roles');
    }
    const now = new Date().toISOString();
    const organization: Organization = {
      id: generateId(),
      version: 0,
      legalName: input.legalName.trim(),
      countryCode: input.countryCode ?? null,
      addressText: input.addressText ?? null,
      registrationNumber: input.registrationNumber ?? null,
      roles: [...input.roles],
      active: input.active ?? true,
      createdAt: now,
      updatedAt: now,
    };
    this.organizations = [...this.organizations, organization];
    return of(clone(organization)).pipe(delay(this.latency));
  }

  update(id: string, patch: UpdateOrganizationRequest): Observable<Organization> {
    const current = this.organizations.find((item) => item.id === id);
    if (!current) {
      return this.fail(404, 'Organization not found.');
    }
    if (current.version !== patch.expectedVersion) {
      return this.fail(409, 'Organization changed; reload before updating.', 'expectedVersion');
    }
    if (patch.roles !== undefined && patch.roles.length === 0) {
      return this.fail(400, 'At least one role is required.', 'roles');
    }
    const next: Organization = {
      ...current,
      legalName: patch.legalName !== undefined ? patch.legalName.trim() : current.legalName,
      countryCode: patch.countryCode !== undefined ? patch.countryCode : current.countryCode,
      addressText: patch.addressText !== undefined ? patch.addressText : current.addressText,
      registrationNumber:
        patch.registrationNumber !== undefined
          ? patch.registrationNumber
          : current.registrationNumber,
      roles: patch.roles !== undefined ? [...patch.roles] : current.roles,
      active: patch.active ?? current.active,
    };
    if (sameOrganization(next, current)) {
      return of(clone(current)).pipe(delay(this.latency));
    }
    next.version = current.version + 1;
    next.updatedAt = new Date().toISOString();
    this.organizations = this.organizations.map((item) => (item.id === id ? next : item));
    return of(clone(next)).pipe(delay(this.latency));
  }

  private fail(status: number, detail: string, field?: string): Observable<never> {
    const problem = {
      type: 'about:blank',
      title: status === 404 ? 'Not found' : status === 409 ? 'Conflict' : 'Invalid request',
      status,
      detail,
      ...(field ? { errors: [{ field, code: 'invalid', message: detail }] } : {}),
    };
    return throwError(() => new ApiProblemError(status, problem)).pipe(delay(this.latency));
  }
}

function sameOrganization(a: Organization, b: Organization): boolean {
  return (
    a.legalName === b.legalName &&
    a.countryCode === b.countryCode &&
    a.addressText === b.addressText &&
    a.registrationNumber === b.registrationNumber &&
    a.active === b.active &&
    sameRoles(a.roles, b.roles)
  );
}

function sameRoles(a: string[], b: string[]): boolean {
  return a.length === b.length && a.every((role) => b.includes(role));
}

function clone<T>(value: T): T {
  return structuredClone(value);
}

function generateId(): string {
  if (typeof crypto !== 'undefined' && 'randomUUID' in crypto) {
    return crypto.randomUUID();
  }
  return `id-${Math.random().toString(36).slice(2)}-${Date.now()}`;
}
