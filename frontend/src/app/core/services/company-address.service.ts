import { InjectionToken } from '@angular/core';
import { Observable } from 'rxjs';
import { Address, AddressPatch, AddressWrite } from '../models/master-data.model';

/**
 * Structured company addresses (`docs/contracts/master-data-v1.yaml`):
 *
 *   GET   /api/organizations/{organizationId}/addresses
 *   POST  /api/organizations/{organizationId}/addresses        (ADMIN/OPERATOR)
 *   PATCH /api/organizations/{organizationId}/addresses/{id}   (ADMIN/OPERATOR,
 *         requires expectedVersion)
 *
 * The legacy `addressText` field remains readable but is not edited here.
 */
export interface CompanyAddressService {
  list(organizationId: string): Observable<Address[]>;
  create(organizationId: string, input: AddressWrite): Observable<Address>;
  update(organizationId: string, addressId: string, patch: AddressPatch): Observable<Address>;
}

export const COMPANY_ADDRESS_SERVICE = new InjectionToken<CompanyAddressService>(
  'COMPANY_ADDRESS_SERVICE',
);
