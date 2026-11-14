import { Inject, Injectable, Optional, inject } from '@angular/core';
import { Observable, of, throwError } from 'rxjs';
import { delay } from 'rxjs/operators';
import {
  FORM_KEYS,
  FormKey,
  OperatorAssignment,
  ProjectPermissions,
} from '../models/account.model';
import { ApiProblemError } from '../models/project.model';
import { AssignmentService } from './assignment.service';
import { AuthStore } from './auth.store';
import { DEFAULT_MOCK_LATENCY_MS, MOCK_LATENCY_MS } from './project-mock.service';

/**
 * In-memory {@link AssignmentService} for offline development. Effective
 * permissions are derived from the signed-in mock account so the permission-
 * aware UI can be demonstrated without a backend.
 */
@Injectable()
export class AssignmentMockService implements AssignmentService {
  private readonly auth = inject(AuthStore);
  private assignments = new Map<string, Map<string, FormKey[]>>();
  private readonly latency: number;

  constructor(@Optional() @Inject(MOCK_LATENCY_MS) latency?: number) {
    this.latency = latency ?? DEFAULT_MOCK_LATENCY_MS;
  }

  listOperators(projectId: string): Observable<OperatorAssignment[]> {
    const project = this.assignments.get(projectId) ?? new Map();
    const items = [...project.entries()]
      .map(([userId, formKeys]) => ({ projectId, userId, formKeys: [...formKeys] }))
      .sort((a, b) => a.userId.localeCompare(b.userId));
    return of(items).pipe(delay(this.latency));
  }

  assign(projectId: string, userId: string, formKeys: FormKey[]): Observable<OperatorAssignment> {
    const project = this.assignments.get(projectId) ?? new Map<string, FormKey[]>();
    project.set(userId, [...new Set(formKeys)]);
    this.assignments.set(projectId, project);
    return of({ projectId, userId, formKeys: [...new Set(formKeys)] }).pipe(delay(this.latency));
  }

  unassign(projectId: string, userId: string): Observable<void> {
    this.assignments.get(projectId)?.delete(userId);
    return of(undefined).pipe(delay(this.latency));
  }

  permissions(projectId: string): Observable<ProjectPermissions> {
    const user = this.auth.user();
    if (!user) {
      return throwError(
        () =>
          new ApiProblemError(401, {
            type: 'about:blank',
            title: 'Authentication required',
            status: 401,
          }),
      ).pipe(delay(this.latency));
    }
    if (user.roles.includes('ADMIN')) {
      return of({
        projectId,
        canRead: true,
        canManageAssignments: true,
        editableForms: [...FORM_KEYS],
      }).pipe(delay(this.latency));
    }
    const editableForms = this.assignments.get(projectId)?.get(user.id) ?? [];
    return of({
      projectId,
      canRead: true,
      canManageAssignments: false,
      editableForms: [...editableForms],
    }).pipe(delay(this.latency));
  }
}
