import { InjectionToken } from '@angular/core';
import { Observable } from 'rxjs';
import {
  CreateProjectRequest,
  Project,
  ProjectListQuery,
  ProjectPage,
  UpdateProjectRequest,
} from '../models/project.model';

/**
 * Typed contract for project operations, mirroring the frozen OpenAPI spec
 * `docs/contracts/projects-v2.yaml`:
 *
 *   POST   /api/projects
 *   GET    /api/projects?page&size&status&q
 *   GET    /api/projects/{id}
 *   PATCH  /api/projects/{id}   (requires expectedVersion)
 *
 * Errors reject with {@link ApiProblemError} carrying RFC 9457 Problem Details.
 * The interface stays transport-agnostic so the mock and HTTP implementations
 * are interchangeable.
 */
export interface ProjectService {
  list(query?: ProjectListQuery): Observable<ProjectPage>;
  get(id: string): Observable<Project>;
  create(input: CreateProjectRequest): Observable<Project>;
  update(id: string, patch: UpdateProjectRequest): Observable<Project>;
}

export const PROJECT_SERVICE = new InjectionToken<ProjectService>('PROJECT_SERVICE');
