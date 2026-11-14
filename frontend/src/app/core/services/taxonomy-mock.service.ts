import { Inject, Injectable, InjectionToken, Optional } from '@angular/core';
import { Observable, of, throwError } from 'rxjs';
import { delay } from 'rxjs/operators';
import {
  ProductTaxonomyCategory,
  ProductTaxonomyCategoryPage,
  ProductTaxonomyCategoryPatch,
  ProductTaxonomyCategoryWrite,
} from '../models/master-data.model';
import { ApiProblemError } from '../models/project.model';
import { DEFAULT_MOCK_LATENCY_MS, MOCK_LATENCY_MS } from './project-mock.service';
import { ReplaceAssignmentsRequest, TaxonomyListQuery, TaxonomyService } from './taxonomy.service';

export interface TaxonomySeed {
  categories: ProductTaxonomyCategory[];
  assignments: Record<string, string[]>;
}

/** Fictional taxonomy categories for offline development; empty by default. */
export const MOCK_TAXONOMY_SEED = new InjectionToken<TaxonomySeed>('MOCK_TAXONOMY_SEED', {
  factory: () => ({ categories: [], assignments: {} }),
});

/** In-memory {@link TaxonomyService}. */
@Injectable()
export class TaxonomyMockService implements TaxonomyService {
  private categories: ProductTaxonomyCategory[];
  private assignments: Record<string, string[]>;
  private readonly latency: number;

  constructor(
    @Optional() @Inject(MOCK_LATENCY_MS) latency?: number,
    @Inject(MOCK_TAXONOMY_SEED) seed: TaxonomySeed = { categories: [], assignments: {} },
  ) {
    this.latency = latency ?? DEFAULT_MOCK_LATENCY_MS;
    this.categories = clone(seed?.categories ?? []);
    this.assignments = clone(seed?.assignments ?? {});
  }

  list(query: TaxonomyListQuery = {}): Observable<ProductTaxonomyCategoryPage> {
    const page = Math.max(0, query.page ?? 0);
    const size = Math.min(100, Math.max(1, query.size ?? 20));
    const prefix = query.q?.trim().toLowerCase();
    const items = this.categories
      .filter((category) => category.active)
      .filter(
        (category) =>
          !prefix ||
          category.name.toLowerCase().startsWith(prefix) ||
          category.code.toLowerCase().startsWith(prefix),
      )
      .sort((a, b) => a.name.localeCompare(b.name) || a.id.localeCompare(b.id));
    const totalElements = items.length;
    const totalPages = totalElements === 0 ? 0 : Math.ceil(totalElements / size);
    return of({
      items: items.slice(page * size, page * size + size).map((item) => clone(item)),
      page,
      size,
      totalElements,
      totalPages,
    }).pipe(delay(this.latency));
  }

  create(input: ProductTaxonomyCategoryWrite): Observable<ProductTaxonomyCategory> {
    if (!input.code?.trim()) {
      return this.fail(400, 'Enter the category code.', 'code');
    }
    if (!input.name?.trim()) {
      return this.fail(400, 'Enter the category name.', 'name');
    }
    if (this.categories.some((item) => item.code === input.code.trim())) {
      return this.fail(409, 'This category code already exists.', 'code');
    }
    const category: ProductTaxonomyCategory = {
      id: generateId(),
      code: input.code.trim(),
      name: input.name.trim(),
      description: input.description?.trim() || null,
      active: true,
      version: 0,
    };
    this.categories = [...this.categories, category];
    return of(clone(category)).pipe(delay(this.latency));
  }

  update(id: string, patch: ProductTaxonomyCategoryPatch): Observable<ProductTaxonomyCategory> {
    const current = this.categories.find((item) => item.id === id);
    if (!current) {
      return this.fail(404, 'Category not found.');
    }
    if (current.version !== patch.expectedVersion) {
      return this.fail(409, 'Category changed; reload before updating.', 'expectedVersion');
    }
    const next: ProductTaxonomyCategory = {
      ...current,
      code: patch.code !== undefined ? patch.code.trim() : current.code,
      name: patch.name !== undefined ? patch.name.trim() : current.name,
      description:
        patch.description !== undefined ? patch.description?.trim() || null : current.description,
    };
    if (!next.code || !next.name) {
      return this.fail(400, 'Code and name are required.');
    }
    if (sameCategory(next, current)) {
      return of(clone(current)).pipe(delay(this.latency));
    }
    next.version = current.version + 1;
    this.categories = this.categories.map((item) => (item.id === id ? next : item));
    return of(clone(next)).pipe(delay(this.latency));
  }

  listAssignments(productId: string): Observable<ProductTaxonomyCategory[]> {
    const ids = this.assignments[productId] ?? [];
    return of(
      this.categories.filter((category) => ids.includes(category.id)).map((item) => clone(item)),
    ).pipe(delay(this.latency));
  }

  replaceAssignments(
    productId: string,
    input: ReplaceAssignmentsRequest,
  ): Observable<ProductTaxonomyCategory[]> {
    const unknown = input.categoryIds.find(
      (id) => !this.categories.some((category) => category.id === id),
    );
    if (unknown) {
      return this.fail(400, 'Unknown taxonomy category.', 'categoryIds');
    }
    this.assignments = { ...this.assignments, [productId]: [...new Set(input.categoryIds)] };
    return this.listAssignments(productId);
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

function sameCategory(a: ProductTaxonomyCategory, b: ProductTaxonomyCategory): boolean {
  return a.code === b.code && a.name === b.name && a.description === b.description;
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
