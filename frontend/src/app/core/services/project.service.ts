import { InjectionToken } from '@angular/core';
import { Observable } from 'rxjs';
import {
  CreateProjectRequest,
  Project,
  ProjectListQuery,
  UpdateProjectRequest,
} from '../models/project.model';

/**
 * Typed contract for project operations.
 *
 * Endpoints follow the representative API in `docs/MASTER-PLAN.md` §10:
 *   POST   /api/projects
 *   GET    /api/projects
 *   GET    /api/projects/{id}
 *   PATCH  /api/projects/{id}
 *
 * The interface is intentionally transport-agnostic so the mock and HTTP
 * implementations are interchangeable.
 */
export interface ProjectService {
  list(query?: ProjectListQuery): Observable<Project[]>;
  get(id: string): Observable<Project>;
  create(input: CreateProjectRequest): Observable<Project>;
  update(id: string, patch: UpdateProjectRequest): Observable<Project>;
}

export const PROJECT_SERVICE = new InjectionToken<ProjectService>('PROJECT_SERVICE');
