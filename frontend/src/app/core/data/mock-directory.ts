import { Country, Organization, Port } from '../models/directory.model';
import { COUNTRY_CODES, countryName } from './countries';
import { PUBLIC_PORTS } from './public-ports';

/**
 * Master-data fixtures.
 *
 * Countries and ports are public reference data and are safe to seed. Companies
 * (organizations) are private customer data: they stay empty until an
 * administrator creates them through the application.
 */

export const MOCK_ORGANIZATIONS: Organization[] = [];

export const MOCK_PORTS: Port[] = PUBLIC_PORTS;

export const MOCK_COUNTRIES: Country[] = COUNTRY_CODES.map((code) => ({
  code,
  name: countryName(code),
}));
