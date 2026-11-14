/**
 * Account, role and form-assignment models.
 *
 * API-facing types are derived from the frozen accounts/access contract
 * (`docs/contracts/accounts-access-v1.yaml`, generated into
 * `core/api/accounts-schema.d.ts`). Regenerate with `npm run gen:api`.
 */
import type { components } from '../api/accounts-schema';

export type AccountRole = components['schemas']['UserResponse']['roles'][number];
export type FormKey = components['schemas']['FormKey'];
export type LoginRequest = components['schemas']['LoginRequest'];
export type User = components['schemas']['UserResponse'];
export type UserPage = components['schemas']['UserPage'];
export type CreateUserRequest = components['schemas']['CreateUserRequest'];
export type UpdateUserRequest = components['schemas']['UpdateUserRequest'];
export type ResetPasswordRequest = components['schemas']['ResetPasswordRequest'];
export type ChangePasswordRequest = components['schemas']['ChangePasswordRequest'];
export type OperatorAssignment = components['schemas']['OperatorAssignment'];
export type OperatorAssignmentRequest = components['schemas']['OperatorAssignmentRequest'];
export type ProjectPermissions = components['schemas']['ProjectPermissions'];

export const ACCOUNT_ROLES: AccountRole[] = ['ADMIN', 'OPERATOR', 'REVIEWER', 'VIEWER'];

export const ACCOUNT_ROLE_LABELS: Record<AccountRole, string> = {
  ADMIN: 'Administrator',
  OPERATOR: 'Operator',
  REVIEWER: 'Reviewer',
  VIEWER: 'Viewer',
};

export const ACCOUNT_ROLE_DESCRIPTIONS: Record<AccountRole, string> = {
  ADMIN: 'Manages accounts, shipments and assignments; edits every form.',
  OPERATOR: 'Reads assigned shipments and edits only assigned forms.',
  REVIEWER: 'Read-only access to assigned shipments; future review actions.',
  VIEWER: 'Read-only access to assigned shipments.',
};

/** All form keys, in canonical workflow order (contract `FormKey` enum). */
export const FORM_KEYS: FormKey[] = [
  'PROJECT_SETUP',
  'INVOICE',
  'PACKING_LIST',
  'CATCH_CERTIFICATES',
  'PROCESSING',
  'TRANSPORT',
  'HEALTH_CERTIFICATE',
  'BILL_OF_LADING',
  'CERTIFICATE_OF_ORIGIN',
  'SHIPPING_LIST',
];

export const FORM_KEY_LABELS: Record<FormKey, string> = {
  PROJECT_SETUP: 'Project setup',
  INVOICE: 'Invoice',
  PACKING_LIST: 'Packing list',
  CATCH_CERTIFICATES: 'Catch certificates',
  PROCESSING: 'Processing',
  TRANSPORT: 'Transport',
  HEALTH_CERTIFICATE: 'Health certificate',
  BILL_OF_LADING: 'Draft bill of lading',
  CERTIFICATE_OF_ORIGIN: 'Certificate of origin',
  SHIPPING_LIST: 'Shipping list',
};

/** Username rules: trim, lowercase; 3–254 chars, email-like identifiers allowed. */
export const USERNAME_PATTERN = /^[A-Za-z0-9][A-Za-z0-9._@+-]{2,253}$/;
export const USERNAME_MIN_LENGTH = 3;
export const USERNAME_MAX_LENGTH = 254;
export const DISPLAY_NAME_MAX_LENGTH = 200;
export const PASSWORD_MIN_LENGTH = 12;
export const PASSWORD_MAX_LENGTH = 64;
/** bcrypt rejects inputs above 72 UTF-8 bytes; the contract enforces this bound. */
export const PASSWORD_MAX_BYTES = 72;

export function normalizeUsername(value: string): string {
  return value.trim().toLowerCase();
}

export function isAdmin(user: User | null | undefined): boolean {
  return !!user?.roles.includes('ADMIN');
}

export function hasRole(user: User | null | undefined, role: AccountRole): boolean {
  return !!user?.roles.includes(role);
}

export function utf8ByteLength(value: string): number {
  return new TextEncoder().encode(value).length;
}
