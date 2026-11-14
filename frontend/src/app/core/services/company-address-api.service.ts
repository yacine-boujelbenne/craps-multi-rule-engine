import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { APP_CONFIG } from '../config/app-config';
import { Address, AddressPatch, AddressWrite } from '../models/master-data.model';
import { toApiProblem } from '../utils/problem';
import { CompanyAddressService } from './company-address.service';

/** HTTP implementation of {@link CompanyAddressService}. */
@Injectable()
export class CompanyAddressApiService implements CompanyAddressService {
  private readonly http = inject(HttpClient);
  private readonly base = `${APP_CONFIG.apiBaseUrl}/organizations`;

  list(organizationId: string): Observable<Address[]> {
    return this.http
      .get<Address[]>(`${this.base}/${organizationId}/addresses`, { withCredentials: true })
      .pipe(catchError(toApiProblem));
  }

  create(organizationId: string, input: AddressWrite): Observable<Address> {
    return this.http
      .post<Address>(`${this.base}/${organizationId}/addresses`, input, {
        withCredentials: true,
      })
      .pipe(catchError(toApiProblem));
  }

  update(organizationId: string, addressId: string, patch: AddressPatch): Observable<Address> {
    return this.http
      .patch<Address>(`${this.base}/${organizationId}/addresses/${addressId}`, patch, {
        withCredentials: true,
      })
      .pipe(catchError(toApiProblem));
  }
}
