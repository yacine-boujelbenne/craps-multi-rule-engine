import { Inject, Injectable, InjectionToken, Optional } from '@angular/core';
import { Observable, of, throwError } from 'rxjs';
import { delay } from 'rxjs/operators';
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
import { ApiProblemError } from '../models/project.model';
import { CatalogListQuery, CatalogService } from './catalog.service';
import { DEFAULT_MOCK_LATENCY_MS, MOCK_LATENCY_MS } from './project-mock.service';

/** Fictional products for offline development; empty by default. */
export const MOCK_PRODUCTS_SEED = new InjectionToken<Product[]>('MOCK_PRODUCTS_SEED', {
  factory: () => [],
});

/** Fictional grades for offline development; empty by default. */
export const MOCK_GRADES_SEED = new InjectionToken<Grade[]>('MOCK_GRADES_SEED', {
  factory: () => [],
});

/**
 * In-memory {@link CatalogService} mirroring the contract: case-sensitive code
 * uniqueness, zero-based pagination, active-only listings, versioned PATCH and
 * the grade price/currency pairing rule.
 */
@Injectable()
export class CatalogMockService implements CatalogService {
  private products: Product[];
  private grades: Grade[];
  private readonly latency: number;

  constructor(
    @Optional() @Inject(MOCK_LATENCY_MS) latency?: number,
    @Inject(MOCK_PRODUCTS_SEED) products: Product[] = [],
    @Inject(MOCK_GRADES_SEED) grades: Grade[] = [],
  ) {
    this.latency = latency ?? DEFAULT_MOCK_LATENCY_MS;
    this.products = (products ?? []).map(clone);
    this.grades = (grades ?? []).map(clone);
  }

  listProducts(query: CatalogListQuery = {}): Observable<ProductPage> {
    let items = [...this.products];
    if (!query.includeInactive) {
      items = items.filter((product) => product.active);
    }
    const q = query.q?.trim().toLowerCase();
    if (q) {
      items = items.filter(
        (product) =>
          product.name.toLowerCase().startsWith(q) || product.code.toLowerCase().startsWith(q),
      );
    }
    items.sort((a, b) => a.name.localeCompare(b.name) || a.id.localeCompare(b.id));
    return of(pageOf(items, query)).pipe(delay(this.latency));
  }

  getProduct(id: string): Observable<Product> {
    const product = this.products.find((item) => item.id === id);
    return product
      ? of(clone(product)).pipe(delay(this.latency))
      : this.fail(404, 'Product not found.');
  }

  createProduct(input: CreateProductRequest): Observable<Product> {
    const code = input.code.trim();
    if (this.products.some((product) => product.code === code)) {
      return this.fail(409, 'A product with this code already exists.', 'code');
    }
    const product: Product = {
      id: generateId(),
      version: 0,
      code,
      name: input.name.trim(),
      scientificName: input.scientificName ?? null,
      hsCode: input.hsCode ?? null,
      active: input.active ?? true,
    };
    this.products = [...this.products, product];
    return of(clone(product)).pipe(delay(this.latency));
  }

  updateProduct(id: string, patch: UpdateProductRequest): Observable<Product> {
    const current = this.products.find((item) => item.id === id);
    if (!current) {
      return this.fail(404, 'Product not found.');
    }
    if (current.version !== patch.expectedVersion) {
      return this.fail(409, 'Product changed; reload before updating.', 'expectedVersion');
    }
    const code = patch.code !== undefined ? patch.code.trim() : current.code;
    if (this.products.some((item) => item.id !== id && item.code === code)) {
      return this.fail(409, 'A product with this code already exists.', 'code');
    }
    const next: Product = {
      ...current,
      code,
      name: patch.name !== undefined ? patch.name.trim() : current.name,
      scientificName:
        patch.scientificName !== undefined ? patch.scientificName : current.scientificName,
      hsCode: patch.hsCode !== undefined ? patch.hsCode : current.hsCode,
      active: patch.active ?? current.active,
    };
    if (sameProduct(next, current)) {
      return of(clone(current)).pipe(delay(this.latency));
    }
    next.version = current.version + 1;
    this.products = this.products.map((item) => (item.id === id ? next : item));
    return of(clone(next)).pipe(delay(this.latency));
  }

  listGrades(productId: string, query: CatalogListQuery = {}): Observable<GradePage> {
    if (!this.products.some((product) => product.id === productId)) {
      return this.fail(404, 'Product not found.');
    }
    let items = this.grades.filter((grade) => grade.productId === productId);
    if (!query.includeInactive) {
      items = items.filter((grade) => grade.active);
    }
    const q = query.q?.trim().toLowerCase();
    if (q) {
      items = items.filter(
        (grade) => grade.name.toLowerCase().startsWith(q) || grade.code.toLowerCase().startsWith(q),
      );
    }
    items.sort(
      (a, b) =>
        a.displayOrder - b.displayOrder || a.code.localeCompare(b.code) || a.id.localeCompare(b.id),
    );
    return of(pageOf(items, query)).pipe(delay(this.latency));
  }

  getGrade(productId: string, gradeId: string): Observable<Grade> {
    const grade = this.grades.find((item) => item.id === gradeId && item.productId === productId);
    return grade ? of(clone(grade)).pipe(delay(this.latency)) : this.fail(404, 'Grade not found.');
  }

  createGrade(productId: string, input: CreateGradeRequest): Observable<Grade> {
    if (!this.products.some((product) => product.id === productId)) {
      return this.fail(404, 'Product not found.');
    }
    const code = input.code.trim();
    if (this.grades.some((grade) => grade.productId === productId && grade.code === code)) {
      return this.fail(409, 'A grade with this code already exists.', 'code');
    }
    const pairingError = validatePairing(input.defaultPricePerKg, input.defaultCurrency);
    if (pairingError) {
      return this.fail(400, pairingError.message, pairingError.field);
    }
    const grade: Grade = {
      id: generateId(),
      productId,
      version: 0,
      code,
      name: input.name.trim(),
      displayOrder: input.displayOrder ?? 0,
      defaultCartonNetKg: input.defaultCartonNetKg ?? null,
      defaultPricePerKg: input.defaultPricePerKg ?? null,
      defaultCurrency: input.defaultCurrency ?? null,
      active: input.active ?? true,
    };
    this.grades = [...this.grades, grade];
    return of(clone(grade)).pipe(delay(this.latency));
  }

  updateGrade(productId: string, gradeId: string, patch: UpdateGradeRequest): Observable<Grade> {
    const current = this.grades.find((item) => item.id === gradeId && item.productId === productId);
    if (!current) {
      return this.fail(404, 'Grade not found.');
    }
    if (current.version !== patch.expectedVersion) {
      return this.fail(409, 'Grade changed; reload before updating.', 'expectedVersion');
    }
    const code = patch.code !== undefined ? patch.code.trim() : current.code;
    if (
      this.grades.some(
        (grade) => grade.id !== gradeId && grade.productId === productId && grade.code === code,
      )
    ) {
      return this.fail(409, 'A grade with this code already exists.', 'code');
    }
    const price =
      patch.defaultPricePerKg !== undefined ? patch.defaultPricePerKg : current.defaultPricePerKg;
    const currency =
      patch.defaultCurrency !== undefined ? patch.defaultCurrency : current.defaultCurrency;
    const pairingError = validatePairing(price, currency);
    if (pairingError) {
      return this.fail(400, pairingError.message, pairingError.field);
    }
    const next: Grade = {
      ...current,
      code,
      name: patch.name !== undefined ? patch.name.trim() : current.name,
      displayOrder: patch.displayOrder ?? current.displayOrder,
      defaultCartonNetKg:
        patch.defaultCartonNetKg !== undefined
          ? patch.defaultCartonNetKg
          : current.defaultCartonNetKg,
      defaultPricePerKg: price,
      defaultCurrency: currency,
      active: patch.active ?? current.active,
    };
    if (sameGrade(next, current)) {
      return of(clone(current)).pipe(delay(this.latency));
    }
    next.version = current.version + 1;
    this.grades = this.grades.map((item) => (item.id === gradeId ? next : item));
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

function pageOf<T>(
  items: T[],
  query: CatalogListQuery,
): { items: T[]; page: number; size: number; totalElements: number; totalPages: number } {
  const page = Math.max(0, query.page ?? 0);
  const size = Math.min(100, Math.max(1, query.size ?? 20));
  const totalElements = items.length;
  const totalPages = totalElements === 0 ? 0 : Math.ceil(totalElements / size);
  const start = page * size;
  return {
    items: items.slice(start, start + size).map(clone),
    page,
    size,
    totalElements,
    totalPages,
  };
}

function validatePairing(
  price: string | null | undefined,
  currency: string | null | undefined,
): { field: string; message: string } | null {
  const hasPrice = price !== null && price !== undefined && price !== '';
  const hasCurrency = currency !== null && currency !== undefined && currency !== '';
  if (hasPrice !== hasCurrency) {
    return {
      field: hasPrice ? 'defaultCurrency' : 'defaultPricePerKg',
      message: 'Price per kg and currency must both be set or both be empty.',
    };
  }
  return null;
}

function sameProduct(a: Product, b: Product): boolean {
  return (
    a.code === b.code &&
    a.name === b.name &&
    a.scientificName === b.scientificName &&
    a.hsCode === b.hsCode &&
    a.active === b.active
  );
}

function sameGrade(a: Grade, b: Grade): boolean {
  return (
    a.code === b.code &&
    a.name === b.name &&
    a.displayOrder === b.displayOrder &&
    a.defaultCartonNetKg === b.defaultCartonNetKg &&
    a.defaultPricePerKg === b.defaultPricePerKg &&
    a.defaultCurrency === b.defaultCurrency &&
    a.active === b.active
  );
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
