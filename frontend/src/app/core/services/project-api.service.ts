import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { APP_CONFIG } from '../config/app-config';
import {
  CreateProjectRequest,
  Project,
  ProjectListQuery,
  UpdateProjectRequest,
} from '../models/project.model';
import { ProjectService } from './project.service';

/**
 * HTTP implementation of {@link ProjectService}.
 *
 * Not registered by default while the backend is under construction; wire it
 * in `app.config.ts` once the OpenAPI contract is frozen. Endpoints follow
 * `docs/MASTER-PLAN.md` §10.
 */
@Injectable()
export class ProjectApiService implements ProjectService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${APP_CONFIG.apiBaseUrl}/projects`;

  list(query?: ProjectListQuery): Observable<Project[]> {
    let params = new HttpParams();
    if (query?.search) {
      params = params.set('search', query.search);
    }
    if (query?.status && query.status !== 'ALL') {
      params = params.set('status', query.status);
    }
    return this.http.get<Project[]>(this.baseUrl, { params });
  }

  get(id: string): Observable<Project> {
    return this.http.get<Project>(`${this.baseUrl}/${id}`);
  }

  create(input: CreateProjectRequest): Observable<Project> {
    return this.http.post<Project>(this.baseUrl, input);
  }

  update(id: string, patch: UpdateProjectRequest): Observable<Project> {
    return this.http.patch<Project>(`${this.baseUrl}/${id}`, patch);
  }
}
