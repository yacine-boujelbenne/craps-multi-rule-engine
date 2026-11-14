import type { components } from '../api/master-data-schema';

export type Country = components['schemas']['Country'];
export type City = components['schemas']['City'];
export type CityPage = components['schemas']['CityPage'];

export type Address = components['schemas']['Address'];
export type AddressWrite = components['schemas']['AddressWrite'];
export type AddressPatch = components['schemas']['AddressPatch'];




export type ProjectProduct = components['schemas']['ProjectProduct'];
export type ProjectProductWrite = components['schemas']['ProjectProductWrite'];
export type ProjectProductPatch = components['schemas']['ProjectProductPatch'];

export type ProductTaxonomyCategory = components['schemas']['ProductTaxonomyCategory'];
export type ProductTaxonomyCategoryPage = components['schemas']['ProductTaxonomyCategoryPage'];
export type ProductTaxonomyCategoryWrite = components['schemas']['ProductTaxonomyCategoryWrite'];
export type ProductTaxonomyCategoryPatch = components['schemas']['ProductTaxonomyCategoryPatch'];

// ---- Addresses -------------------------------------------------------------

export type AddressKind = Address['kind'];

export const ADDRESS_KINDS: AddressKind[] = ['BUSINESS', 'FACTORY', 'BILLING', 'OTHER'];

export const ADDRESS_KIND_LABELS: Record<AddressKind, string> = {
  BUSINESS: 'Business address',
  FACTORY: 'Factory address',
  BILLING: 'Billing address',
  OTHER: 'Other address',
};

export const ADDRESS_LINE_MAX_LENGTH = 250;
export const POSTAL_CODE_MAX_LENGTH = 40;
export const CITY_NAME_MAX_LENGTH = 200;

// ---- Product taxonomy ------------------------------------------------------

export const CATEGORY_CODE_MAX_LENGTH = 60;
export const CATEGORY_NAME_MAX_LENGTH = 150;
export const CATEGORY_DESCRIPTION_MAX_LENGTH = 500;

// ---- Shipment products -----------------------------------------------------

export const CARTON_COUNT_MIN = 1;
/** Quantity/weights: up to 15 integer digits and three fractional digits. */
export const PROJECT_QUANTITY_PATTERN = /^[0-9]{1,15}(\.[0-9]{1,3})?$/;
/** Shipment price: up to 14 integer digits and four fractional digits. */
export const PROJECT_PRICE_PATTERN = /^[0-9]{1,14}(\.[0-9]{1,4})?$/;
export const PROJECT_QUANTITY_UNIT_MAX_LENGTH = 30;
export const PROJECT_LOT_REFERENCE_MAX_LENGTH = 150;

export const PROJECT_PRODUCT_MEASUREMENT_FIELDS = [
  'quantity',
  'netWeightKg',
  'grossWeightKg',
] as const;

/** Trims an optional decimal/date string, returning null for blanks. */
export function nullIfBlank(value: string | null | undefined): string | null {
  const trimmed = (value ?? '').trim();
  return trimmed.length === 0 ? null : trimmed;
}

export function normalizeMeasurement(value: string | null | undefined): string | null {
  return nullIfBlank(value);
}

export function normalizePostalCode(value: string | null | undefined): string | null {
  return nullIfBlank(value);
}
