import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { APP_CONFIG } from '../config/app-config';
import { CityPage, Country } from '../models/master-data.model';
import { toApiProblem } from '../utils/problem';
import { CityListQuery, GeographyService } from './geography.service';

/** HTTP implementation of {@link GeographyService}. */
@Injectable()
export class GeographyApiService implements GeographyService {
  private readonly http = inject(HttpClient);
  private readonly base = `${APP_CONFIG.apiBaseUrl}/countries`;

  listCountries(): Observable<Country[]> {
    return this.http
      .get<Country[]>(this.base, { withCredentials: true })
      .pipe(catchError(toApiProblem));
  }

  searchCities(countryCode: string, query: CityListQuery = {}): Observable<CityPage> {
    let params = new HttpParams();
    if (query.q?.trim()) {
      params = params.set('q', query.q.trim());
    }
    if (query.page !== undefined) {
      params = params.set('page', query.page);
    }
    if (query.size !== undefined) {
      params = params.set('size', query.size);
    }
    return this.http
      .get<CityPage>(`${this.base}/${countryCode}/cities`, { params, withCredentials: true })
      .pipe(catchError(toApiProblem));
  }
}
