import { InjectionToken } from '@angular/core';
import { Observable } from 'rxjs';
import {
  ProductTaxonomyCategory,
  ProductTaxonomyCategoryPage,
  ProductTaxonomyCategoryPatch,
  ProductTaxonomyCategoryWrite,
} from '../models/master-data.model';

export interface TaxonomyListQuery {
  q?: string;
  page?: number;
  size?: number;
}

export interface ReplaceAssignmentsRequest {
  expectedProductVersion: number;
  categoryIds: string[];
}

/**
 * Product taxonomy categories (`docs/contracts/master-data-v1.yaml`):
 *
 *   GET   /api/product-taxonomy-categories?q&page&size
 *   POST  /api/product-taxonomy-categories                 (ADMIN/OPERATOR)
 *   PATCH /api/product-taxonomy-categories/{id}            (ADMIN/OPERATOR)
 *   GET   /api/products/{productId}/taxonomy-categories
 *   PUT   /api/products/{productId}/taxonomy-categories    (ADMIN/OPERATOR)
 *
 * Taxonomy categories classify products; they are not packing grades.
 */
export interface TaxonomyService {
  list(query?: TaxonomyListQuery): Observable<ProductTaxonomyCategoryPage>;
  create(input: ProductTaxonomyCategoryWrite): Observable<ProductTaxonomyCategory>;
  update(id: string, patch: ProductTaxonomyCategoryPatch): Observable<ProductTaxonomyCategory>;

  listAssignments(productId: string): Observable<ProductTaxonomyCategory[]>;
  replaceAssignments(
    productId: string,
    input: ReplaceAssignmentsRequest,
  ): Observable<ProductTaxonomyCategory[]>;
}

export const TAXONOMY_SERVICE = new InjectionToken<TaxonomyService>('TAXONOMY_SERVICE');
