import { InjectionToken } from '@angular/core';
import { Observable } from 'rxjs';
import { FormKey, OperatorAssignment, ProjectPermissions } from '../models/account.model';

/**
 * Project membership and per-form edit grants
 * (`docs/contracts/accounts-access-v1.yaml`):
 *
 *   GET    /api/admin/projects/{projectId}/operators
 *   PUT    /api/admin/projects/{projectId}/operators/{userId}   (replace form set)
 *   DELETE /api/admin/projects/{projectId}/operators/{userId}   (revoke membership)
 *   GET    /api/projects/{id}/permissions                       (current user)
 *
 * An empty form set grants read-only project access. The server enforces every
 * rule; the UI only reflects the effective permission set.
 */
export interface AssignmentService {
  listOperators(projectId: string): Observable<OperatorAssignment[]>;
  assign(projectId: string, userId: string, formKeys: FormKey[]): Observable<OperatorAssignment>;
  unassign(projectId: string, userId: string): Observable<void>;
  permissions(projectId: string): Observable<ProjectPermissions>;
}

export const ASSIGNMENT_SERVICE = new InjectionToken<AssignmentService>('ASSIGNMENT_SERVICE');
