import { Inject, Injectable, InjectionToken, Optional } from '@angular/core';
import { Observable, of } from 'rxjs';
import { delay } from 'rxjs/operators';
import { COUNTRY_CODES, countryName } from '../data/countries';
import { PUBLIC_CITY_NAMES_BY_COUNTRY } from '../data/public-cities';
import { City, CityPage, Country } from '../models/master-data.model';
import { stableUuid } from '../utils/stable-id';
import { CityListQuery, GeographyService } from './geography.service';
import { DEFAULT_MOCK_LATENCY_MS, MOCK_LATENCY_MS } from './project-mock.service';

/** Fictional city fixtures for offline development (public reference data). */
export const MOCK_CITIES_SEED = new InjectionToken<City[]>('MOCK_CITIES_SEED', {
  factory: () =>
    Object.entries(PUBLIC_CITY_NAMES_BY_COUNTRY).flatMap(([countryCode, names]) =>
      names.map((name) => city(countryCode, name)),
    ),
});

/** In-memory {@link GeographyService} backed by the ISO country list and fixtures. */
@Injectable()
export class GeographyMockService implements GeographyService {
  private readonly countries: Country[] = COUNTRY_CODES.map((code) => ({
    code,
    name: countryName(code),
  })).sort(
    (a, b) =>
      a.name.toLowerCase().localeCompare(b.name.toLowerCase()) || a.code.localeCompare(b.code),
  );

  private readonly cities: City[];
  private readonly latency: number;

  constructor(
    @Optional() @Inject(MOCK_LATENCY_MS) latency?: number,
    @Inject(MOCK_CITIES_SEED) seed: City[] = [],
  ) {
    this.latency = latency ?? DEFAULT_MOCK_LATENCY_MS;
    this.cities = (seed ?? []).map((item) => ({ ...item }));
  }

  listCountries(): Observable<Country[]> {
    return of(this.countries.map((item) => ({ ...item }))).pipe(delay(this.latency));
  }

  searchCities(countryCode: string, query: CityListQuery = {}): Observable<CityPage> {
    const page = Math.max(0, query.page ?? 0);
    const size = Math.min(100, Math.max(1, query.size ?? 20));
    const prefix = query.q?.trim().toLowerCase();
    const items = this.cities
      .filter((item) => item.countryCode === countryCode && item.active)
      .filter((item) => !prefix || item.name.toLowerCase().startsWith(prefix))
      .sort(
        (a, b) =>
          a.name.toLowerCase().localeCompare(b.name.toLowerCase()) || a.id.localeCompare(b.id),
      );
    const totalElements = items.length;
    const totalPages = totalElements === 0 ? 0 : Math.ceil(totalElements / size);
    return of({
      items: items.slice(page * size, page * size + size).map((item) => ({ ...item })),
      page,
      size,
      totalElements,
      totalPages,
    }).pipe(delay(this.latency));
  }
}

function city(countryCode: string, name: string): City {
  return {
    id: stableUuid(`${countryCode}:${name}`),
    countryCode,
    name,
    active: true,
  };
}
