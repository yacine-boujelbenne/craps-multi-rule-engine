import { Country, Organization, Port } from '../models/directory.model';

/**
 * Master-data fixtures.
 *
 * Deliberately empty: the directory endpoints do not exist yet, and the
 * application must not ship fabricated organizations, ports or countries.
 * Development and tests provide their own data through the mock services.
 */

export const MOCK_ORGANIZATIONS: Organization[] = [];

export const MOCK_PORTS: Port[] = [];

export const MOCK_COUNTRIES: Country[] = [];
