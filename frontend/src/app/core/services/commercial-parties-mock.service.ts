import { Inject, Injectable, InjectionToken, Optional } from '@angular/core';
import { Observable, of, throwError } from 'rxjs';
import { delay } from 'rxjs/operators';
import { CommercialParties, UpdateCommercialPartiesRequest } from '../models/commercial.model';
import { ApiProblemError } from '../models/project.model';
import { CommercialPartiesService } from './commercial-parties.service';
import { DEFAULT_MOCK_LATENCY_MS, MOCK_LATENCY_MS } from './project-mock.service';

/** Seed commercial parties keyed by project id; empty by default. */
export const MOCK_COMMERCIAL_PARTIES_SEED = new InjectionToken<Record<string, CommercialParties>>(
  'MOCK_COMMERCIAL_PARTIES_SEED',
  { factory: () => ({}) },
);

/**
 * In-memory {@link CommercialPartiesService}. Each project keeps one record
 * with a monotonic version; PATCH is atomic and never mutates on a no-op.
 */
@Injectable()
export class CommercialPartiesMockService implements CommercialPartiesService {
  private readonly store = new Map<string, CommercialParties>();
  private readonly latency: number;

  constructor(
    @Optional() @Inject(MOCK_LATENCY_MS) latency?: number,
    @Inject(MOCK_COMMERCIAL_PARTIES_SEED) seed: Record<string, CommercialParties> = {},
  ) {
    this.latency = latency ?? DEFAULT_MOCK_LATENCY_MS;
    for (const [projectId, parties] of Object.entries(seed ?? {})) {
      this.store.set(projectId, structuredClone(parties));
    }
  }

  get(projectId: string): Observable<CommercialParties> {
    return of(this.snapshot(projectId)).pipe(delay(this.latency));
  }

  update(projectId: string, patch: UpdateCommercialPartiesRequest): Observable<CommercialParties> {
    const current = this.snapshot(projectId);
    if (patch.expectedVersion !== current.version) {
      return throwError(
        () =>
          new ApiProblemError(409, {
            type: 'about:blank',
            title: 'Stale version',
            status: 409,
            detail: `Expected version ${patch.expectedVersion} but the shipment is at ${current.version}.`,
            errors: [
              {
                field: 'expectedVersion',
                code: 'stale',
                message: 'The shipment changed since it was loaded.',
              },
            ],
          }),
      ).pipe(delay(this.latency));
    }
    const next: CommercialParties = {
      projectId,
      version: current.version,
      supplierId: patch.supplierId !== undefined ? patch.supplierId : current.supplierId,
      clientId: patch.clientId !== undefined ? patch.clientId : current.clientId,
      importerId: patch.importerId !== undefined ? patch.importerId : current.importerId,
      consigneeId: patch.consigneeId !== undefined ? patch.consigneeId : current.consigneeId,
    };
    const unchanged =
      next.supplierId === current.supplierId &&
      next.clientId === current.clientId &&
      next.importerId === current.importerId &&
      next.consigneeId === current.consigneeId;
    if (unchanged) {
      return of(current).pipe(delay(this.latency));
    }
    next.version = current.version + 1;
    this.store.set(projectId, next);
    return of(structuredClone(next)).pipe(delay(this.latency));
  }

  private snapshot(projectId: string): CommercialParties {
    const existing = this.store.get(projectId);
    if (existing) {
      return structuredClone(existing);
    }
    const created: CommercialParties = {
      projectId,
      version: 0,
      supplierId: null,
      clientId: null,
      importerId: null,
      consigneeId: null,
    };
    this.store.set(projectId, created);
    return structuredClone(created);
  }
}
