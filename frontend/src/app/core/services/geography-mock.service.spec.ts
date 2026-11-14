import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { beforeEach, describe, expect, it } from 'vitest';
import { COUNTRY_CODES } from '../data/countries';
import { GeographyMockService } from './geography-mock.service';
import { MOCK_LATENCY_MS } from './project-mock.service';

describe('GeographyMockService', () => {
  let service: GeographyMockService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [{ provide: MOCK_LATENCY_MS, useValue: 0 }, GeographyMockService],
    });
    service = TestBed.inject(GeographyMockService);
  });

  it('lists every ISO country as public reference data', async () => {
    const countries = await firstValueFrom(service.listCountries());
    expect(countries).toHaveLength(COUNTRY_CODES.length);
    expect(countries.find((country) => country.code === 'MA')?.name).toBe('Morocco');
    expect(countries.find((country) => country.code === 'FR')?.name).toBe('France');
  });

  it('seeds real city names for each country', async () => {
    const morocco = await firstValueFrom(service.searchCities('MA', { size: 100 }));
    expect(morocco.items.map((city) => city.name)).toEqual(
      expect.arrayContaining(['Agadir', 'Casablanca', 'Tangier']),
    );
    expect(morocco.totalElements).toBeGreaterThan(10);

    const france = await firstValueFrom(service.searchCities('FR', { q: 'le h', size: 100 }));
    expect(france.items.some((city) => city.name === 'Le Havre')).toBe(true);
  });

  it('pages and orders cities by name', async () => {
    const first = await firstValueFrom(service.searchCities('US', { size: 10 }));
    const second = await firstValueFrom(service.searchCities('US', { page: 1, size: 10 }));
    expect(first.items).toHaveLength(10);
    expect(first.items.map((city) => city.name)).toEqual(
      [...first.items].map((city) => city.name).sort((a, b) => a.localeCompare(b)),
    );
    expect(second.items[0].id).not.toBe(first.items[0].id);
  });
});
