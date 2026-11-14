/**
 * Commercial-setup models (organizations, products/grades and shipment
 * commercial parties).
 *
 * API-facing types are derived from the frozen contract
 * (`docs/contracts/commercial-setup-v1.yaml`, generated into
 * `core/api/commercial-schema.d.ts`). Regenerate with `npm run gen:api`.
 */
import type { components } from '../api/commercial-schema';

export type OrganizationRole = components['schemas']['OrganizationRole'] | 'CARRIER';

type OrganizationBase = components['schemas']['OrganizationResponse'];
type OrganizationMetadataFields = {
  tradeName?: string | null;
  taxIdentifier?: string | null;
  email?: string | null;
  phone?: string | null;
};

export type Organization = Omit<OrganizationBase, 'roles'> &
  OrganizationMetadataFields & { roles: OrganizationRole[] };
export type OrganizationPage = Omit<components['schemas']['OrganizationPage'], 'items'> & {
  items: Organization[];
};
export type CreateOrganizationRequest = Omit<
  components['schemas']['CreateOrganizationRequest'],
  'roles'
> &
  OrganizationMetadataFields & { roles: OrganizationRole[] };
export type UpdateOrganizationRequest = Omit<
  components['schemas']['UpdateOrganizationRequest'],
  'roles'
> &
  OrganizationMetadataFields & { roles?: OrganizationRole[] };

type ProductMetadataFields = {
  description?: string | null;
  processingMethod?: string | null;
  storageRequirements?: string | null;
  defaultMeasurementUnit?: string | null;
};

export type Product = components['schemas']['ProductResponse'] & ProductMetadataFields;
export type ProductPage = Omit<components['schemas']['ProductPage'], 'items'> & {
  items: Product[];
};
export type CreateProductRequest = components['schemas']['CreateProductRequest'] &
  ProductMetadataFields;
export type UpdateProductRequest = components['schemas']['UpdateProductRequest'] &
  ProductMetadataFields;

export type Grade = components['schemas']['GradeResponse'];
export type GradePage = components['schemas']['GradePage'];
export type CreateGradeRequest = components['schemas']['CreateGradeRequest'];
export type UpdateGradeRequest = components['schemas']['UpdateGradeRequest'];

export type CommercialParties = components['schemas']['CommercialPartiesResponse'];
export type UpdateCommercialPartiesRequest =
  components['schemas']['UpdateCommercialPartiesRequest'];

/** Canonical order of organization roles (contract enum order). */
export const ORGANIZATION_ROLES: OrganizationRole[] = [
  'EXPORTER',
  'PRODUCER',
  'SUPPLIER',
  'BUYER',
  'CONSIGNEE',
  'NOTIFY_PARTY',
  'FREIGHT_FORWARDER',
  'SHIPPING_LINE',
  'CARRIER',
  'AUTHORITY',
  'CLIENT',
  'IMPORTER',
];

export const ORGANIZATION_ROLE_LABELS: Record<OrganizationRole, string> = {
  EXPORTER: 'Exporter',
  PRODUCER: 'Producer',
  SUPPLIER: 'Supplier',
  BUYER: 'Buyer',
  CONSIGNEE: 'Consignee',
  NOTIFY_PARTY: 'Notify party',
  FREIGHT_FORWARDER: 'Freight forwarder',
  SHIPPING_LINE: 'Shipping line',
  CARRIER: 'Carrier',
  AUTHORITY: 'Authority',
  CLIENT: 'Client',
  IMPORTER: 'Importer',
};

export const ORGANIZATION_ROLE_DESCRIPTIONS: Record<OrganizationRole, string> = {
  EXPORTER: 'Ships the goods and appears on the export documents.',
  PRODUCER: 'Farms or processes the product (origin of the catch).',
  SUPPLIER: 'Provides goods or raw material for the shipment.',
  BUYER: 'Purchases the goods commercially.',
  CONSIGNEE: 'Receives the shipment at destination.',
  NOTIFY_PARTY: 'Notified when the shipment arrives.',
  FREIGHT_FORWARDER: 'Arranges transport and documentation.',
  SHIPPING_LINE: 'Shipping line operating the vessel.',
  CARRIER: 'Carrier issuing the bill of lading.',
  AUTHORITY: 'Official or regulatory body.',
  CLIENT: 'Commercial client of the exporter for this shipment.',
  IMPORTER: 'Imports the goods at the destination country.',
};

/** Party slots on a shipment, each mapping to a required organization role. */
export interface CommercialPartySlot {
  key: 'supplierId' | 'clientId' | 'importerId' | 'consigneeId';
  role: OrganizationRole;
  label: string;
}

export const COMMERCIAL_PARTY_SLOTS: CommercialPartySlot[] = [
  { key: 'supplierId', role: 'SUPPLIER', label: 'Supplier' },
  { key: 'clientId', role: 'CLIENT', label: 'Client' },
  { key: 'importerId', role: 'IMPORTER', label: 'Importer' },
  { key: 'consigneeId', role: 'CONSIGNEE', label: 'Consignee' },
];

// ---- Field bounds and patterns (mirrors the contract) -----------------------

export const LEGAL_NAME_MAX_LENGTH = 250;
export const ADDRESS_MAX_LENGTH = 4000;
export const REGISTRATION_NUMBER_MAX_LENGTH = 100;
export const TRADE_NAME_MAX_LENGTH = 250;
export const TAX_IDENTIFIER_MAX_LENGTH = 100;
export const EMAIL_MAX_LENGTH = 254;
export const PHONE_MAX_LENGTH = 50;
export const COUNTRY_CODE_PATTERN = /^[A-Z]{2}$/;

export const CODE_PATTERN = /^[A-Za-z0-9][A-Za-z0-9._/+-]{0,79}$/;
export const CODE_MAX_LENGTH = 80;
export const PRODUCT_NAME_MAX_LENGTH = 250;
export const SCIENTIFIC_NAME_MAX_LENGTH = 250;
export const HS_CODE_PATTERN = /^[0-9]{6,10}$/;
export const GRADE_NAME_MAX_LENGTH = 200;
export const PROCESSING_METHOD_MAX_LENGTH = 150;
export const STORAGE_REQUIREMENTS_MAX_LENGTH = 500;
export const MEASUREMENT_UNIT_MAX_LENGTH = 30;

/** Positive decimal kg string, up to three fractional digits (numeric(18,3)). */
export const CARTON_NET_KG_PATTERN = /^(?!0(?:\.0{1,3})?$)(?:0|[1-9][0-9]{0,14})(?:\.[0-9]{1,3})?$/;
/** Nonnegative decimal per-kg price string, up to four fractional digits (numeric(18,4)). */
export const PRICE_PER_KG_PATTERN = /^(?:0|[1-9][0-9]{0,13})(?:\.[0-9]{1,4})?$/;
export const CURRENCY_PATTERN = /^[A-Z]{3}$/;

export function normalizeCode(value: string): string {
  return value.trim();
}

export function normalizeCountryCode(value: string): string {
  return value.trim().toUpperCase();
}

export function normalizeCurrency(value: string): string {
  return value.trim().toUpperCase();
}

/** Trims an optional string, returning null for blanks. */
export function nullIfBlank(value: string | null | undefined): string | null {
  const trimmed = (value ?? '').trim();
  return trimmed.length === 0 ? null : trimmed;
}
