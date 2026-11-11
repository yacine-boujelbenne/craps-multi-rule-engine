import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { APP_CONFIG } from '../config/app-config';
import {
  ApiProblemError,
  CreateProjectRequest,
  Problem,
  Project,
  ProjectListQuery,
  ProjectPage,
  UpdateProjectRequest,
} from '../models/project.model';
import { ProjectService } from './project.service';

/**
 * HTTP implementation of {@link ProjectService} targeting the frozen contract
 * (`docs/contracts/projects-v2.yaml`). Session auth uses cookies, so requests
 * are sent with credentials; CSRF is handled by the auth milestone.
 *
 * Not registered by default while the backend is under construction; enable via
 * `APP_CONFIG.useMockApi = false` once it is reachable.
 */
@Injectable()
export class ProjectApiService implements ProjectService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${APP_CONFIG.apiBaseUrl}/projects`;

  list(query: ProjectListQuery = {}): Observable<ProjectPage> {
    let params = new HttpParams();
    if (query.page !== undefined) {
      params = params.set('page', query.page);
    }
    if (query.size !== undefined) {
      params = params.set('size', query.size);
    }
    if (query.status) {
      params = params.set('status', query.status);
    }
    if (query.q?.trim()) {
      params = params.set('q', query.q.trim());
    }
    return this.http
      .get<ProjectPage>(this.baseUrl, { params, withCredentials: true })
      .pipe(catchError((error: unknown) => this.toProblem(error)));
  }

  get(id: string): Observable<Project> {
    return this.http
      .get<Project>(`${this.baseUrl}/${id}`, { withCredentials: true })
      .pipe(catchError((error: unknown) => this.toProblem(error)));
  }

  create(input: CreateProjectRequest): Observable<Project> {
    return this.http
      .post<Project>(this.baseUrl, input, { withCredentials: true })
      .pipe(catchError((error: unknown) => this.toProblem(error)));
  }

  update(id: string, patch: UpdateProjectRequest): Observable<Project> {
    return this.http
      .patch<Project>(`${this.baseUrl}/${id}`, patch, { withCredentials: true })
      .pipe(catchError((error: unknown) => this.toProblem(error)));
  }

  private toProblem(error: unknown): Observable<never> {
    if (error instanceof HttpErrorResponse) {
      const body = error.error;
      if (body && typeof body === 'object' && typeof body.title === 'string') {
        return throwError(() => new ApiProblemError(error.status, body as Problem));
      }
      return throwError(
        () =>
          new ApiProblemError(error.status, {
            type: 'about:blank',
            title: error.statusText || 'Request failed',
            status: error.status,
          }),
      );
    }
    return throwError(() => error);
  }
}
