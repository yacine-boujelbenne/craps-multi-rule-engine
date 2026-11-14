import { InjectionToken } from '@angular/core';
import { Observable } from 'rxjs';
import {
  CreateGradeRequest,
  CreateProductRequest,
  Grade,
  GradePage,
  Product,
  ProductPage,
  UpdateGradeRequest,
  UpdateProductRequest,
} from '../models/commercial.model';

export interface CatalogListQuery {
  page?: number;
  size?: number;
  q?: string;
  includeInactive?: boolean;
}

/**
 * Product/grade catalog (`docs/contracts/commercial-setup-v1.yaml`):
 *
 *   GET    /api/products?page&size&q&includeInactive
 *   POST   /api/products                              (ADMIN)
 *   GET    /api/products/{id}
 *   PATCH  /api/products/{id}                         (ADMIN, expectedVersion)
 *   GET    /api/products/{productId}/grades
 *   POST   /api/products/{productId}/grades           (ADMIN)
 *   GET    /api/products/{productId}/grades/{gradeId}
 *   PATCH  /api/products/{productId}/grades/{gradeId} (ADMIN, expectedVersion)
 *
 * Reads are open to authenticated read roles; all writes require ADMIN.
 */
export interface CatalogService {
  listProducts(query?: CatalogListQuery): Observable<ProductPage>;
  getProduct(id: string): Observable<Product>;
  createProduct(input: CreateProductRequest): Observable<Product>;
  updateProduct(id: string, patch: UpdateProductRequest): Observable<Product>;

  listGrades(productId: string, query?: CatalogListQuery): Observable<GradePage>;
  getGrade(productId: string, gradeId: string): Observable<Grade>;
  createGrade(productId: string, input: CreateGradeRequest): Observable<Grade>;
  updateGrade(productId: string, gradeId: string, patch: UpdateGradeRequest): Observable<Grade>;
}

export const CATALOG_SERVICE = new InjectionToken<CatalogService>('CATALOG_SERVICE');
