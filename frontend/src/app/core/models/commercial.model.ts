/**
 * Commercial-setup models (organizations, products/grades and shipment
 * commercial parties).
 *
 * API-facing types are derived from the frozen contract
 * (`docs/contracts/commercial-setup-v1.yaml`, generated into
 * `core/api/commercial-schema.d.ts`). Regenerate with `npm run gen:api`.
 */
import type { components } from '../api/commercial-schema';

export type OrganizationRole = components['schemas']['OrganizationRole'];
export type Organization = components['schemas']['OrganizationResponse'];
export type OrganizationPage = components['schemas']['OrganizationPage'];
export type CreateOrganizationRequest = components['schemas']['CreateOrganizationRequest'];
export type UpdateOrganizationRequest = components['schemas']['UpdateOrganizationRequest'];

export type Product = components['schemas']['ProductResponse'];
export type ProductPage = components['schemas']['ProductPage'];
export type CreateProductRequest = components['schemas']['CreateProductRequest'];
export type UpdateProductRequest = components['schemas']['UpdateProductRequest'];

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
  SHIPPING_LINE: 'Carrier issuing the bill of lading.',
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
export const COUNTRY_CODE_PATTERN = /^[A-Z]{2}$/;

export const CODE_PATTERN = /^[A-Za-z0-9][A-Za-z0-9._/+-]{0,79}$/;
export const CODE_MAX_LENGTH = 80;
export const PRODUCT_NAME_MAX_LENGTH = 250;
export const SCIENTIFIC_NAME_MAX_LENGTH = 250;
export const HS_CODE_PATTERN = /^[0-9]{6,10}$/;
export const GRADE_NAME_MAX_LENGTH = 200;

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
