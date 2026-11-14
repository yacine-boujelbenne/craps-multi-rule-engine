import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { APP_CONFIG } from '../config/app-config';
import {
  ProjectProduct,
  ProjectProductPatch,
  ProjectProductWrite,
} from '../models/master-data.model';
import { toApiProblem } from '../utils/problem';
import { ShipmentProductService } from './shipment-product.service';

/** HTTP implementation of {@link ShipmentProductService}. */
@Injectable()
export class ShipmentProductApiService implements ShipmentProductService {
  private readonly http = inject(HttpClient);
  private readonly base = `${APP_CONFIG.apiBaseUrl}/projects`;

  listProducts(projectId: string): Observable<ProjectProduct[]> {
    return this.http
      .get<ProjectProduct[]>(`${this.base}/${projectId}/products`, { withCredentials: true })
      .pipe(catchError(toApiProblem));
  }

  addProduct(projectId: string, input: ProjectProductWrite): Observable<ProjectProduct> {
    return this.http
      .post<ProjectProduct>(`${this.base}/${projectId}/products`, input, {
        withCredentials: true,
      })
      .pipe(catchError(toApiProblem));
  }

  updateProduct(
    projectId: string,
    projectProductId: string,
    patch: ProjectProductPatch,
  ): Observable<ProjectProduct> {
    return this.http
      .patch<ProjectProduct>(`${this.base}/${projectId}/products/${projectProductId}`, patch, {
        withCredentials: true,
      })
      .pipe(catchError(toApiProblem));
  }
}
