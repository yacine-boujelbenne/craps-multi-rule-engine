import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { APP_CONFIG } from '../config/app-config';
import { FormKey, OperatorAssignment, ProjectPermissions } from '../models/account.model';
import { toApiProblem } from '../utils/problem';
import { AssignmentService } from './assignment.service';

/** HTTP implementation of {@link AssignmentService}. */
@Injectable()
export class AssignmentApiService implements AssignmentService {
  private readonly http = inject(HttpClient);
  private readonly adminBase = `${APP_CONFIG.apiBaseUrl}/admin/projects`;

  listOperators(projectId: string): Observable<OperatorAssignment[]> {
    return this.http
      .get<OperatorAssignment[]>(`${this.adminBase}/${projectId}/operators`, {
        withCredentials: true,
      })
      .pipe(catchError(toApiProblem));
  }

  assign(projectId: string, userId: string, formKeys: FormKey[]): Observable<OperatorAssignment> {
    return this.http
      .put<OperatorAssignment>(
        `${this.adminBase}/${projectId}/operators/${userId}`,
        { formKeys },
        { withCredentials: true },
      )
      .pipe(catchError(toApiProblem));
  }

  unassign(projectId: string, userId: string): Observable<void> {
    return this.http
      .delete<void>(`${this.adminBase}/${projectId}/operators/${userId}`, { withCredentials: true })
      .pipe(catchError(toApiProblem));
  }

  permissions(projectId: string): Observable<ProjectPermissions> {
    return this.http
      .get<ProjectPermissions>(`${APP_CONFIG.apiBaseUrl}/projects/${projectId}/permissions`, {
        withCredentials: true,
      })
      .pipe(catchError(toApiProblem));
  }
}
