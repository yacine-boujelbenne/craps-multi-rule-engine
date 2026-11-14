import { InjectionToken } from '@angular/core';
import { Observable } from 'rxjs';
import { CityPage, Country } from '../models/master-data.model';

export interface CityListQuery {
  q?: string;
  page?: number;
  size?: number;
}

/**
 * Country/city reference data (`docs/contracts/master-data-v1.yaml`):
 *
 *   GET /api/countries
 *   GET /api/countries/{countryCode}/cities?q&page&size
 *
 * Reads are available to any authenticated role.
 */
export interface GeographyService {
  listCountries(): Observable<Country[]>;
  searchCities(countryCode: string, query?: CityListQuery): Observable<CityPage>;
}

export const GEOGRAPHY_SERVICE = new InjectionToken<GeographyService>('GEOGRAPHY_SERVICE');
