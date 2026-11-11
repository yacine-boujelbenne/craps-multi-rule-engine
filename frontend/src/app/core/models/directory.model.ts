/**
 * Master-data directory models.
 *
 * Party and port fields on the project contract are UUIDs referencing
 * `organization.id` / `port.id`. No directory endpoints exist yet, so the UI is
 * served by fictional development fixtures (see `core/data/mock-directory.ts`).
 * Never place real customer data in the repository.
 */

export type OrganizationRole =
  | 'EXPORTER'
  | 'PRODUCER'
  | 'SUPPLIER'
  | 'BUYER'
  | 'CONSIGNEE'
  | 'NOTIFY_PARTY'
  | 'FREIGHT_FORWARDER'
  | 'SHIPPING_LINE'
  | 'AUTHORITY';

export interface Organization {
  id: string;
  legalName: string;
  countryCode: string | null;
  roles: OrganizationRole[];
  active: boolean;
}

export interface Port {
  id: string;
  name: string;
  countryCode: string;
  unLocode: string | null;
  active: boolean;
}

export interface Country {
  code: string;
  name: string;
}
