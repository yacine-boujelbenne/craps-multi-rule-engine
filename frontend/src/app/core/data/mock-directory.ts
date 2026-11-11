import { Country, Organization, Port } from '../models/directory.model';

/**
 * Fictional master-data fixtures.
 *
 * Development-only stand-ins for the directory endpoints that do not exist yet
 * (`organizations`, `ports`, `countries`). All names and identifiers are
 * invented; never place real customer data here.
 */

export const MOCK_ORGANIZATIONS: Organization[] = [
  {
    id: '00000000-0000-4000-8000-000000000101',
    legalName: 'Atlantic Blue Seafood Co.',
    countryCode: 'MA',
    roles: ['EXPORTER', 'SUPPLIER'],
    active: true,
  },
  {
    id: '00000000-0000-4000-8000-000000000102',
    legalName: 'Nordic Catch Processing',
    countryCode: 'MA',
    roles: ['PRODUCER'],
    active: true,
  },
  {
    id: '00000000-0000-4000-8000-000000000103',
    legalName: 'Sahara Fisheries',
    countryCode: 'MA',
    roles: ['PRODUCER'],
    active: true,
  },
  {
    id: '00000000-0000-4000-8000-000000000201',
    legalName: 'Iberia Import S.L.',
    countryCode: 'ES',
    roles: ['CONSIGNEE', 'NOTIFY_PARTY'],
    active: true,
  },
  {
    id: '00000000-0000-4000-8000-000000000202',
    legalName: 'Mediterraneo Food Group',
    countryCode: 'IT',
    roles: ['CONSIGNEE'],
    active: true,
  },
  {
    id: '00000000-0000-4000-8000-000000000203',
    legalName: 'Tokyo Marine Foods K.K.',
    countryCode: 'JP',
    roles: ['CONSIGNEE', 'NOTIFY_PARTY'],
    active: true,
  },
  {
    id: '00000000-0000-4000-8000-000000000204',
    legalName: 'Hanseatic Seafood GmbH',
    countryCode: 'DE',
    roles: ['CONSIGNEE'],
    active: true,
  },
  {
    id: '00000000-0000-4000-8000-000000000301',
    legalName: 'Adriatic Customs Broker',
    countryCode: 'IT',
    roles: ['NOTIFY_PARTY'],
    active: true,
  },
  {
    id: '00000000-0000-4000-8000-000000000302',
    legalName: 'Barcelona Fresh Logistics',
    countryCode: 'ES',
    roles: ['NOTIFY_PARTY'],
    active: true,
  },
  {
    id: '00000000-0000-4000-8000-000000000401',
    legalName: 'TransGlobal Logistics',
    countryCode: 'MA',
    roles: ['FREIGHT_FORWARDER'],
    active: true,
  },
  {
    id: '00000000-0000-4000-8000-000000000402',
    legalName: 'EuroFreight AG',
    countryCode: 'DE',
    roles: ['FREIGHT_FORWARDER'],
    active: true,
  },
  {
    id: '00000000-0000-4000-8000-000000000501',
    legalName: 'Maersk Line',
    countryCode: 'DK',
    roles: ['SHIPPING_LINE'],
    active: true,
  },
  {
    id: '00000000-0000-4000-8000-000000000502',
    legalName: 'CMA CGM',
    countryCode: 'FR',
    roles: ['SHIPPING_LINE'],
    active: true,
  },
  {
    id: '00000000-0000-4000-8000-000000000503',
    legalName: 'MSC',
    countryCode: 'CH',
    roles: ['SHIPPING_LINE'],
    active: true,
  },
  {
    id: '00000000-0000-4000-8000-000000000504',
    legalName: 'Hapag-Lloyd',
    countryCode: 'DE',
    roles: ['SHIPPING_LINE'],
    active: true,
  },
  {
    id: '00000000-0000-4000-8000-000000000505',
    legalName: 'Ocean Network Express (ONE)',
    countryCode: 'JP',
    roles: ['SHIPPING_LINE'],
    active: true,
  },
];

export const MOCK_PORTS: Port[] = [
  {
    id: '00000000-0000-4000-8000-000000000601',
    name: 'Tanger Med',
    countryCode: 'MA',
    unLocode: 'MAPTM',
    active: true,
  },
  {
    id: '00000000-0000-4000-8000-000000000602',
    name: 'Agadir',
    countryCode: 'MA',
    unLocode: 'MAAGA',
    active: true,
  },
  {
    id: '00000000-0000-4000-8000-000000000603',
    name: 'Casablanca',
    countryCode: 'MA',
    unLocode: 'MACAS',
    active: true,
  },
  {
    id: '00000000-0000-4000-8000-000000000701',
    name: 'Vigo',
    countryCode: 'ES',
    unLocode: 'ESVGO',
    active: true,
  },
  {
    id: '00000000-0000-4000-8000-000000000702',
    name: 'Barcelona',
    countryCode: 'ES',
    unLocode: 'ESBCN',
    active: true,
  },
  {
    id: '00000000-0000-4000-8000-000000000703',
    name: 'Genoa',
    countryCode: 'IT',
    unLocode: 'ITGOA',
    active: true,
  },
  {
    id: '00000000-0000-4000-8000-000000000704',
    name: 'Le Havre',
    countryCode: 'FR',
    unLocode: 'FRLEH',
    active: true,
  },
  {
    id: '00000000-0000-4000-8000-000000000705',
    name: 'Hamburg',
    countryCode: 'DE',
    unLocode: 'DEHAM',
    active: true,
  },
  {
    id: '00000000-0000-4000-8000-000000000706',
    name: 'Yokohama',
    countryCode: 'JP',
    unLocode: 'JPYOK',
    active: true,
  },
  {
    id: '00000000-0000-4000-8000-000000000707',
    name: 'Newark',
    countryCode: 'US',
    unLocode: 'USEWR',
    active: true,
  },
  {
    id: '00000000-0000-4000-8000-000000000708',
    name: 'Jebel Ali',
    countryCode: 'AE',
    unLocode: 'AEJEA',
    active: true,
  },
];

export const MOCK_COUNTRIES: Country[] = [
  { code: 'AE', name: 'United Arab Emirates' },
  { code: 'CH', name: 'Switzerland' },
  { code: 'DE', name: 'Germany' },
  { code: 'DK', name: 'Denmark' },
  { code: 'ES', name: 'Spain' },
  { code: 'FR', name: 'France' },
  { code: 'IT', name: 'Italy' },
  { code: 'JP', name: 'Japan' },
  { code: 'MA', name: 'Morocco' },
  { code: 'US', name: 'United States' },
];
