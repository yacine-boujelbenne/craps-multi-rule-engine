import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { APP_CONFIG } from '../config/app-config';
import { CommercialParties, UpdateCommercialPartiesRequest } from '../models/commercial.model';
import { toApiProblem } from '../utils/problem';
import { CommercialPartiesService } from './commercial-parties.service';

/** HTTP implementation of {@link CommercialPartiesService}. */
@Injectable()
export class CommercialPartiesApiService implements CommercialPartiesService {
  private readonly http = inject(HttpClient);
  private readonly base = `${APP_CONFIG.apiBaseUrl}/projects`;

  get(projectId: string): Observable<CommercialParties> {
    return this.http
      .get<CommercialParties>(`${this.base}/${projectId}/commercial-parties`, {
        withCredentials: true,
      })
      .pipe(catchError(toApiProblem));
  }

  update(projectId: string, patch: UpdateCommercialPartiesRequest): Observable<CommercialParties> {
    return this.http
      .patch<CommercialParties>(`${this.base}/${projectId}/commercial-parties`, patch, {
        withCredentials: true,
      })
      .pipe(catchError(toApiProblem));
  }
}
