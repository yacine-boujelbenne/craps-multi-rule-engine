import { InjectionToken } from '@angular/core';
import { Observable } from 'rxjs';
import {
  ProjectProduct,
  ProjectProductPatch,
  ProjectProductWrite,
} from '../models/master-data.model';

/**
 * Shipment master-data links (`docs/contracts/master-data-v1.yaml`):
 *
 *   GET  /api/projects/{projectId}/products
 *   POST /api/projects/{projectId}/products                     (PACKING_LIST)
 *   PATCH /api/projects/{projectId}/products/{projectProductId} (PACKING_LIST)
 *
 * Every mutation carries `expectedProjectVersion` and returns the shared
 * project revision. Shipment product prices are ADMIN-only.
 */
export interface ShipmentProductService {

  listProducts(projectId: string): Observable<ProjectProduct[]>;
  addProduct(projectId: string, input: ProjectProductWrite): Observable<ProjectProduct>;
  updateProduct(
    projectId: string,
    projectProductId: string,
    patch: ProjectProductPatch,
  ): Observable<ProjectProduct>;
}

export const SHIPMENT_PRODUCT_SERVICE = new InjectionToken<ShipmentProductService>(
  'SHIPMENT_PRODUCT_SERVICE',
);
