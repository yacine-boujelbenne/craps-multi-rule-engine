/**
 * Domain models for the export documentation platform.
 *
 * The API-facing types are derived from the frozen OpenAPI contract
 * (`docs/contracts/projects-v2.yaml`, generated into `core/api/schema.d.ts`).
 * Regenerate with `npm run gen:api` whenever the contract version changes.
 *
 * Field ownership follows `docs/01-field-ownership.md`. One project is exactly
 * one shipment.
 */
import type { components } from '../api/schema';

export type ProjectStatus = components['schemas']['ProjectStatus'];
export type Project = components['schemas']['ProjectResponse'];
export type ProjectPage = components['schemas']['ProjectPage'];
export type CreateProjectRequest = components['schemas']['CreateProjectRequest'];
export type UpdateProjectRequest = components['schemas']['UpdateProjectRequest'];
export type Problem = components['schemas']['Problem'];
export type ProblemField = NonNullable<Problem['errors']>[number];

export const PROJECT_STATUSES: ProjectStatus[] = [
  'DRAFT_DATA',
  'IN_PROGRESS',
  'READY_FOR_DRAFT_REVIEW',
  'DRAFT_REVIEWED',
  'AWAITING_EXTERNAL_ISSUANCE',
  'OFFICIAL_DOCUMENTS_RECEIVED',
  'COMPLETE',
  'ARCHIVED',
];

export type StatusTone = 'neutral' | 'info' | 'progress' | 'warning' | 'success' | 'danger';

export interface StatusMeta {
  label: string;
  tone: StatusTone;
}

export const PROJECT_STATUS_META: Record<ProjectStatus, StatusMeta> = {
  DRAFT_DATA: { label: 'Draft', tone: 'neutral' },
  IN_PROGRESS: { label: 'In progress', tone: 'progress' },
  READY_FOR_DRAFT_REVIEW: { label: 'Awaiting validation', tone: 'warning' },
  DRAFT_REVIEWED: { label: 'Draft reviewed', tone: 'info' },
  AWAITING_EXTERNAL_ISSUANCE: { label: 'Awaiting issuance', tone: 'info' },
  OFFICIAL_DOCUMENTS_RECEIVED: { label: 'Official docs received', tone: 'success' },
  COMPLETE: { label: 'Complete', tone: 'success' },
  ARCHIVED: { label: 'Archived', tone: 'neutral' },
};

/** Status groups used to derive dashboard figures (no dedicated stats endpoint). */
export const DRAFT_STATUSES: ProjectStatus[] = ['DRAFT_DATA'];
export const IN_PROGRESS_STATUSES: ProjectStatus[] = ['IN_PROGRESS'];
export const AWAITING_VALIDATION_STATUSES: ProjectStatus[] = [
  'READY_FOR_DRAFT_REVIEW',
  'DRAFT_REVIEWED',
];
export const COMPLETED_STATUSES: ProjectStatus[] = [
  'AWAITING_EXTERNAL_ISSUANCE',
  'OFFICIAL_DOCUMENTS_RECEIVED',
  'COMPLETE',
  'ARCHIVED',
];

export interface DashboardStats {
  totalProjects: number;
  draftProjects: number;
  inProgress: number;
  awaitingValidation: number;
  completed: number;
}

export interface ProjectListQuery {
  page?: number;
  size?: number;
  status?: ProjectStatus;
  q?: string;
}

/**
 * Reference format from the contract: trim outer whitespace before validation,
 * store uppercase. Globally unique including archived projects.
 */
export const REFERENCE_PATTERN = /^[A-Za-z0-9][A-Za-z0-9._/-]{0,79}$/;
export const COUNTRY_CODE_PATTERN = /^[A-Z]{2}$/;
export const NOTES_MAX_LENGTH = 2000;

export function normalizeReference(value: string): string {
  return value.trim().toUpperCase();
}

/**
 * RFC 9457 Problem Details surfaced by the API. Mock and HTTP transports both
 * reject with this so callers can map `errors[].field` to form controls.
 */
export class ApiProblemError extends Error {
  readonly status: number;
  readonly problem: Problem;

  constructor(status: number, problem: Problem) {
    super(problem.title || `Request failed with status ${status}`);
    this.name = 'ApiProblemError';
    this.status = status;
    this.problem = problem;
  }

  get fieldErrors(): ProblemField[] {
    return this.problem.errors ?? [];
  }

  /** Returns the first message for a given JSON field name, if any. */
  messageFor(field: string): string | null {
    return this.fieldErrors.find((error) => error.field === field)?.message ?? null;
  }
}

export function isApiProblemError(value: unknown): value is ApiProblemError {
  return value instanceof ApiProblemError;
}
