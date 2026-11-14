import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { APP_CONFIG } from '../config/app-config';
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
import { toApiProblem } from '../utils/problem';
import { CatalogListQuery, CatalogService } from './catalog.service';

/** HTTP implementation of {@link CatalogService}. */
@Injectable()
export class CatalogApiService implements CatalogService {
  private readonly http = inject(HttpClient);
  private readonly base = `${APP_CONFIG.apiBaseUrl}/products`;

  listProducts(query: CatalogListQuery = {}): Observable<ProductPage> {
    return this.http
      .get<ProductPage>(this.base, { params: this.params(query), withCredentials: true })
      .pipe(catchError(toApiProblem));
  }

  getProduct(id: string): Observable<Product> {
    return this.http
      .get<Product>(`${this.base}/${id}`, { withCredentials: true })
      .pipe(catchError(toApiProblem));
  }

  createProduct(input: CreateProductRequest): Observable<Product> {
    return this.http
      .post<Product>(this.base, input, { withCredentials: true })
      .pipe(catchError(toApiProblem));
  }

  updateProduct(id: string, patch: UpdateProductRequest): Observable<Product> {
    return this.http
      .patch<Product>(`${this.base}/${id}`, patch, { withCredentials: true })
      .pipe(catchError(toApiProblem));
  }

  listGrades(productId: string, query: CatalogListQuery = {}): Observable<GradePage> {
    return this.http
      .get<GradePage>(`${this.base}/${productId}/grades`, {
        params: this.params(query),
        withCredentials: true,
      })
      .pipe(catchError(toApiProblem));
  }

  getGrade(productId: string, gradeId: string): Observable<Grade> {
    return this.http
      .get<Grade>(`${this.base}/${productId}/grades/${gradeId}`, { withCredentials: true })
      .pipe(catchError(toApiProblem));
  }

  createGrade(productId: string, input: CreateGradeRequest): Observable<Grade> {
    return this.http
      .post<Grade>(`${this.base}/${productId}/grades`, input, { withCredentials: true })
      .pipe(catchError(toApiProblem));
  }

  updateGrade(productId: string, gradeId: string, patch: UpdateGradeRequest): Observable<Grade> {
    return this.http
      .patch<Grade>(`${this.base}/${productId}/grades/${gradeId}`, patch, {
        withCredentials: true,
      })
      .pipe(catchError(toApiProblem));
  }

  private params(query: CatalogListQuery): HttpParams {
    let params = new HttpParams();
    if (query.page !== undefined) {
      params = params.set('page', query.page);
    }
    if (query.size !== undefined) {
      params = params.set('size', query.size);
    }
    if (query.q?.trim()) {
      params = params.set('q', query.q.trim());
    }
    if (query.includeInactive) {
      params = params.set('includeInactive', true);
    }
    if (query.categoryId) {
      params = params.set('categoryId', query.categoryId);
    }
    return params;
  }
}
