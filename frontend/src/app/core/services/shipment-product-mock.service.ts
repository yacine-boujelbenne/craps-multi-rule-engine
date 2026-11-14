import { Inject, Injectable, InjectionToken, Optional } from '@angular/core';
import { Observable, of, throwError } from 'rxjs';
import { catchError, delay, map } from 'rxjs/operators';
import {
  ProjectProduct,
  ProjectProductPatch,
  ProjectProductWrite,
} from '../models/master-data.model';
import { ApiProblemError } from '../models/project.model';
import { ShipmentProductService } from './shipment-product.service';
import { DEFAULT_MOCK_LATENCY_MS, MOCK_LATENCY_MS } from './project-mock.service';

export interface ShipmentProductSeed {
  projectVersions?: Record<string, number>;
  products?: Record<string, ProjectProduct[]>;
}

/** Fictional shipment product lines for offline development. */
export const MOCK_SHIPMENT_PRODUCT_SEED = new InjectionToken<ShipmentProductSeed>(
  'MOCK_SHIPMENT_PRODUCT_SEED',
  { factory: () => ({}) },
);

/** In-memory {@link ShipmentProductService} with shared project revisions. */
@Injectable()
export class ShipmentProductMockService implements ShipmentProductService {
  private readonly versions: Record<string, number>;
  private products: Record<string, ProjectProduct[]>;
  private readonly latency: number;

  constructor(
    @Optional() @Inject(MOCK_LATENCY_MS) latency?: number,
    @Inject(MOCK_SHIPMENT_PRODUCT_SEED) seed: ShipmentProductSeed = {},
  ) {
    this.latency = latency ?? DEFAULT_MOCK_LATENCY_MS;
    this.versions = { ...(seed?.projectVersions ?? {}) };
    this.products = clone(seed?.products ?? {});
  }

  listProducts(projectId: string): Observable<ProjectProduct[]> {
    return of(clone(this.products[projectId] ?? [])).pipe(delay(this.latency));
  }

  addProduct(projectId: string, input: ProjectProductWrite): Observable<ProjectProduct> {
    const version = this.versionOf(projectId, input.expectedProjectVersion);
    if (version !== input.expectedProjectVersion) {
      return this.fail(
        409,
        'The shipment changed; reload before saving.',
        'expectedProjectVersion',
      );
    }
    const invalid = validateProduct(input);
    if (invalid) {
      return this.fail(400, invalid.message, invalid.field);
    }
    const nextVersion = version + 1;
    this.versions[projectId] = nextVersion;
    const product: ProjectProduct = {
      id: generateId(),
      version: 0,
      projectVersion: nextVersion,
      productId: input.productId,
      productCategoryId: input.productCategoryId ?? null,
      cartonCount: input.cartonCount ?? null,
      quantity: input.quantity ?? null,
      quantityUnit: input.quantityUnit?.trim() || null,
      netWeightKg: input.netWeightKg ?? null,
      grossWeightKg: input.grossWeightKg ?? null,
      unitPrice: input.unitPrice ?? null,
      currency: input.currency ?? null,
      lotReference: input.lotReference?.trim() || null,
      processingInformation: input.processingInformation?.trim() || null,
      appliedGradeVersion: null,
      appliedGradeCode: null,
      appliedGradeName: null,
      appliedCartonNetKg: null,
    };
    this.products = {
      ...this.products,
      [projectId]: [...(this.products[projectId] ?? []), product],
    };
    return of(clone(product)).pipe(delay(this.latency));
  }

  updateProduct(
    projectId: string,
    projectProductId: string,
    patch: ProjectProductPatch,
  ): Observable<ProjectProduct> {
    const current = (this.products[projectId] ?? []).find((item) => item.id === projectProductId);
    if (!current) {
      return this.fail(404, 'Shipment product not found.');
    }
    if (current.version !== patch.expectedVersion) {
      return this.fail(409, 'The product line changed; reload before saving.', 'expectedVersion');
    }
    const version = this.versionOf(projectId, patch.expectedProjectVersion);
    if (version !== patch.expectedProjectVersion) {
      return this.fail(
        409,
        'The shipment changed; reload before saving.',
        'expectedProjectVersion',
      );
    }
    const categoryChanged =
      patch.productCategoryId !== undefined &&
      patch.productCategoryId !== current.productCategoryId;
    const next: ProjectProduct = {
      ...current,
      productCategoryId:
        patch.productCategoryId !== undefined ? patch.productCategoryId : current.productCategoryId,
      cartonCount: patch.cartonCount !== undefined ? patch.cartonCount : current.cartonCount,
      quantity: patch.quantity !== undefined ? patch.quantity : current.quantity,
      quantityUnit:
        patch.quantityUnit !== undefined
          ? patch.quantityUnit?.trim() || null
          : current.quantityUnit,
      netWeightKg: patch.netWeightKg !== undefined ? patch.netWeightKg : current.netWeightKg,
      grossWeightKg:
        patch.grossWeightKg !== undefined ? patch.grossWeightKg : current.grossWeightKg,
      unitPrice: patch.unitPrice !== undefined ? patch.unitPrice : current.unitPrice,
      currency: patch.currency !== undefined ? patch.currency : current.currency,
      lotReference:
        patch.lotReference !== undefined
          ? patch.lotReference?.trim() || null
          : current.lotReference,
      processingInformation:
        patch.processingInformation !== undefined
          ? patch.processingInformation?.trim() || null
          : current.processingInformation,
      appliedGradeVersion: categoryChanged ? null : current.appliedGradeVersion,
      appliedGradeCode: categoryChanged ? null : current.appliedGradeCode,
      appliedGradeName: categoryChanged ? null : current.appliedGradeName,
      appliedCartonNetKg: categoryChanged ? null : current.appliedCartonNetKg,
    };
    const invalid = validateProduct(next);
    if (invalid) {
      return this.fail(400, invalid.message, invalid.field);
    }
    if (sameProduct(next, current)) {
      return of(clone(current)).pipe(delay(this.latency));
    }
    const nextVersion = version + 1;
    this.versions[projectId] = nextVersion;
    const saved: ProjectProduct = {
      ...next,
      version: current.version + 1,
      projectVersion: nextVersion,
    };
    this.products = {
      ...this.products,
      [projectId]: (this.products[projectId] ?? []).map((item) =>
        item.id === projectProductId ? saved : item,
      ),
    };
    return of(clone(saved)).pipe(delay(this.latency));
  }

  private versionOf(projectId: string, expected: number): number {
    return this.versions[projectId] ?? expected;
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

function validateProduct(
  input: Pick<
    ProjectProductWrite,
    'quantity' | 'netWeightKg' | 'grossWeightKg' | 'unitPrice' | 'currency'
  >,
): { field: string; message: string } | null {
  const decimal = /^[0-9]{1,15}(\.[0-9]{1,3})?$/;
  for (const field of ['quantity', 'netWeightKg', 'grossWeightKg'] as const) {
    const value = input[field];
    if (value != null && !decimal.test(value)) {
      return { field, message: 'Use a non-negative decimal with up to three decimals.' };
    }
  }
  if (input.unitPrice != null && !/^[0-9]{1,14}(\.[0-9]{1,4})?$/.test(input.unitPrice)) {
    return { field: 'unitPrice', message: 'Use a non-negative price with up to four decimals.' };
  }
  const hasPrice = input.unitPrice != null;
  const hasCurrency = input.currency != null;
  if (hasPrice !== hasCurrency) {
    return {
      field: hasPrice ? 'currency' : 'unitPrice',
      message: 'Provide both price and currency.',
    };
  }
  if (input.currency != null && !/^[A-Z]{3}$/.test(input.currency)) {
    return { field: 'currency', message: 'Use a three-letter ISO currency code.' };
  }
  return null;
}

function sameProduct(a: ProjectProduct, b: ProjectProduct): boolean {
  return (
    a.productCategoryId === b.productCategoryId &&
    a.cartonCount === b.cartonCount &&
    a.quantity === b.quantity &&
    a.quantityUnit === b.quantityUnit &&
    a.netWeightKg === b.netWeightKg &&
    a.grossWeightKg === b.grossWeightKg &&
    a.unitPrice === b.unitPrice &&
    a.currency === b.currency &&
    a.lotReference === b.lotReference &&
    a.processingInformation === b.processingInformation
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
