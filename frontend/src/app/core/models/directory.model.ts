
import type { OrganizationRole } from './commercial.model';

export type { OrganizationRole };

/** Lightweight organization projection used by directory lookups/selectors. */
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
