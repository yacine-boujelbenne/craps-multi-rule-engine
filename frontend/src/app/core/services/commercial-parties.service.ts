import { InjectionToken } from '@angular/core';
import { Observable } from 'rxjs';
import { CommercialParties, UpdateCommercialPartiesRequest } from '../models/commercial.model';

/**
 * Shipment commercial parties (`docs/contracts/commercial-setup-v1.yaml`):
 *
 *   GET   /api/projects/{id}/commercial-parties
 *   PATCH /api/projects/{id}/commercial-parties   (ADMIN or PROJECT_SETUP)
 *
 * All four party ids are nullable and independent. The response `version` is
 * the shared project revision; PATCH is atomic and increments it at most once.
 */
export interface CommercialPartiesService {
  get(projectId: string): Observable<CommercialParties>;
  update(projectId: string, patch: UpdateCommercialPartiesRequest): Observable<CommercialParties>;
}

export const COMMERCIAL_PARTIES_SERVICE = new InjectionToken<CommercialPartiesService>(
  'COMMERCIAL_PARTIES_SERVICE',
);
