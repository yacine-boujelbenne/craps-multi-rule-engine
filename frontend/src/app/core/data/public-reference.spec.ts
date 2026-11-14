import { describe, expect, it } from 'vitest';
import { COUNTRY_DIAL_CODES, countryDialCode } from './country-dial-codes';
import { COUNTRY_CODES } from './countries';
import { MOCK_COUNTRIES, MOCK_ORGANIZATIONS, MOCK_PORTS } from './mock-directory';
import { PUBLIC_CITY_NAMES_BY_COUNTRY } from './public-cities';

describe('public reference data', () => {
  it('covers every country with public city names except uninhabited islands', () => {
    const missing = COUNTRY_CODES.filter(
      (code) => !PUBLIC_CITY_NAMES_BY_COUNTRY[code]?.length && code !== 'BV' && code !== 'HM',
    );
    expect(missing).toEqual([]);
    expect(PUBLIC_CITY_NAMES_BY_COUNTRY['MA']).toEqual(
      expect.arrayContaining(['Agadir', 'Casablanca', 'Tangier']),
    );
  });

  it('provides calling codes for all countries with a dial plan', () => {
    const missing = COUNTRY_CODES.filter(
      (code) => !COUNTRY_DIAL_CODES[code] && code !== 'AQ' && code !== 'HM',
    );
    expect(missing).toEqual([]);
    expect(countryDialCode('ma')).toBe('+212');
    expect(countryDialCode('FR')).toBe('+33');
    expect(countryDialCode('US')).toBe('+1');
    expect(countryDialCode('ZZ')).toBeNull();
    expect(countryDialCode(null)).toBeNull();
  });

  it('seeds countries and ports but never private organizations', () => {
    expect(MOCK_ORGANIZATIONS).toEqual([]);
    expect(MOCK_COUNTRIES).toHaveLength(COUNTRY_CODES.length);
    expect(MOCK_PORTS.length).toBeGreaterThan(100);
    expect(MOCK_PORTS.find((port) => port.unLocode === 'MAPTM')?.name).toBe('Tangier Med');
    expect(new Set(MOCK_PORTS.map((port) => port.id)).size).toBe(MOCK_PORTS.length);
  });
});
