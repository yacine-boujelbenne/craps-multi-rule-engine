import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { APP_CONFIG } from '../config/app-config';
import {
  ProductTaxonomyCategory,
  ProductTaxonomyCategoryPage,
  ProductTaxonomyCategoryPatch,
  ProductTaxonomyCategoryWrite,
} from '../models/master-data.model';
import { toApiProblem } from '../utils/problem';
import { ReplaceAssignmentsRequest, TaxonomyListQuery, TaxonomyService } from './taxonomy.service';

/** HTTP implementation of {@link TaxonomyService}. */
@Injectable()
export class TaxonomyApiService implements TaxonomyService {
  private readonly http = inject(HttpClient);
  private readonly categories = `${APP_CONFIG.apiBaseUrl}/product-taxonomy-categories`;
  private readonly products = `${APP_CONFIG.apiBaseUrl}/products`;

  list(query: TaxonomyListQuery = {}): Observable<ProductTaxonomyCategoryPage> {
    let params = new HttpParams();
    if (query.q?.trim()) {
      params = params.set('q', query.q.trim());
    }
    if (query.page !== undefined) {
      params = params.set('page', query.page);
    }
    if (query.size !== undefined) {
      params = params.set('size', query.size);
    }
    return this.http
      .get<ProductTaxonomyCategoryPage>(this.categories, { params, withCredentials: true })
      .pipe(catchError(toApiProblem));
  }

  create(input: ProductTaxonomyCategoryWrite): Observable<ProductTaxonomyCategory> {
    return this.http
      .post<ProductTaxonomyCategory>(this.categories, input, { withCredentials: true })
      .pipe(catchError(toApiProblem));
  }

  update(id: string, patch: ProductTaxonomyCategoryPatch): Observable<ProductTaxonomyCategory> {
    return this.http
      .patch<ProductTaxonomyCategory>(`${this.categories}/${id}`, patch, {
        withCredentials: true,
      })
      .pipe(catchError(toApiProblem));
  }

  listAssignments(productId: string): Observable<ProductTaxonomyCategory[]> {
    return this.http
      .get<ProductTaxonomyCategory[]>(`${this.products}/${productId}/taxonomy-categories`, {
        withCredentials: true,
      })
      .pipe(catchError(toApiProblem));
  }

  replaceAssignments(
    productId: string,
    input: ReplaceAssignmentsRequest,
  ): Observable<ProductTaxonomyCategory[]> {
    return this.http
      .put<ProductTaxonomyCategory[]>(`${this.products}/${productId}/taxonomy-categories`, input, {
        withCredentials: true,
      })
      .pipe(catchError(toApiProblem));
  }
}
