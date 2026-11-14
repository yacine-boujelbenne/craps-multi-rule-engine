import { Inject, Injectable, InjectionToken, Optional } from '@angular/core';
import { Observable, of, throwError } from 'rxjs';
import { delay } from 'rxjs/operators';
import { Address, AddressPatch, AddressWrite } from '../models/master-data.model';
import { ApiProblemError } from '../models/project.model';
import { CompanyAddressService } from './company-address.service';
import { DEFAULT_MOCK_LATENCY_MS, MOCK_LATENCY_MS } from './project-mock.service';

/** Fictional company addresses for offline development; empty by default. */
export const MOCK_COMPANY_ADDRESSES_SEED = new InjectionToken<Record<string, Address[]>>(
  'MOCK_COMPANY_ADDRESSES_SEED',
  { factory: () => ({}) },
);

/** In-memory {@link CompanyAddressService} with versioned PATCH semantics. */
@Injectable()
export class CompanyAddressMockService implements CompanyAddressService {
  private addresses: Record<string, Address[]>;
  private readonly latency: number;

  constructor(
    @Optional() @Inject(MOCK_LATENCY_MS) latency?: number,
    @Inject(MOCK_COMPANY_ADDRESSES_SEED) seed: Record<string, Address[]> = {},
  ) {
    this.latency = latency ?? DEFAULT_MOCK_LATENCY_MS;
    this.addresses = structuredClone(seed ?? {});
  }

  list(organizationId: string): Observable<Address[]> {
    return of(clone(this.addresses[organizationId] ?? [])).pipe(delay(this.latency));
  }

  create(organizationId: string, input: AddressWrite): Observable<Address> {
    const invalid = validateAddress(input);
    if (invalid) {
      return this.fail(400, invalid.message, invalid.field);
    }
    const address: Address = {
      id: generateId(),
      organizationId,
      kind: input.kind,
      countryCode: input.countryCode,
      cityId: input.cityId ?? null,
      manualCity: input.manualCity ?? null,
      addressLine1: input.addressLine1.trim(),
      addressLine2: input.addressLine2?.trim() || null,
      postalCode: input.postalCode?.trim() || null,
      active: true,
      version: 0,
    };
    this.addresses = {
      ...this.addresses,
      [organizationId]: [...(this.addresses[organizationId] ?? []), address],
    };
    return of(clone(address)).pipe(delay(this.latency));
  }

  update(organizationId: string, addressId: string, patch: AddressPatch): Observable<Address> {
    const current = (this.addresses[organizationId] ?? []).find((item) => item.id === addressId);
    if (!current) {
      return this.fail(404, 'Address not found.');
    }
    if (current.version !== patch.expectedVersion) {
      return this.fail(409, 'Address changed; reload before updating.', 'expectedVersion');
    }
    const next: Address = {
      ...current,
      kind: patch.kind ?? current.kind,
      countryCode: patch.countryCode ?? current.countryCode,
      cityId: patch.cityId !== undefined ? patch.cityId : current.cityId,
      manualCity: patch.manualCity !== undefined ? patch.manualCity : current.manualCity,
      addressLine1:
        patch.addressLine1 !== undefined ? patch.addressLine1.trim() : current.addressLine1,
      addressLine2:
        patch.addressLine2 !== undefined
          ? patch.addressLine2?.trim() || null
          : current.addressLine2,
      postalCode:
        patch.postalCode !== undefined ? patch.postalCode?.trim() || null : current.postalCode,
    };
    const invalid = validateAddress(next);
    if (invalid) {
      return this.fail(400, invalid.message, invalid.field);
    }
    next.version = current.version + 1;
    this.addresses = {
      ...this.addresses,
      [organizationId]: (this.addresses[organizationId] ?? []).map((item) =>
        item.id === addressId ? next : item,
      ),
    };
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

function validateAddress(
  input: Pick<AddressWrite, 'countryCode' | 'cityId' | 'manualCity' | 'addressLine1'>,
): { field: string; message: string } | null {
  if (!/^[A-Z]{2}$/.test(input.countryCode)) {
    return { field: 'countryCode', message: 'Use an ISO 3166-1 alpha-2 country code.' };
  }
  if (!input.addressLine1?.trim()) {
    return { field: 'addressLine1', message: 'Enter the street address.' };
  }
  const hasCity = Boolean(input.cityId);
  const hasManual = Boolean(input.manualCity?.trim());
  if (hasCity === hasManual) {
    return { field: 'cityId', message: 'Choose a city or enter the city name manually.' };
  }
  return null;
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
