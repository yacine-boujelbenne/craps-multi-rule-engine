/**
 * Domain models for the export documentation platform.
 *
 * These interfaces mirror the shared API contract. Until the backend OpenAPI
 * spec is published they are served by an isolated mock implementation
 * (see `core/services`). Field ownership follows `docs/01-field-ownership.md`.
 */

export type ProjectStatus =
  | 'DRAFT'
  | 'IN_PROGRESS'
  | 'READY_FOR_DRAFT_REVIEW'
  | 'DRAFT_REVIEWED'
  | 'AWAITING_EXTERNAL_ISSUANCE'
  | 'OFFICIAL_DOCUMENTS_RECEIVED'
  | 'COMPLETE'
  | 'ARCHIVED';

export type DocumentStatus =
  'NOT_STARTED' | 'IN_PROGRESS' | 'READY' | 'DRAFT' | 'STALE' | 'APPROVED' | 'EXTERNAL_ISSUED';

/** Chronological document workflow keys (navigation placeholders only for now). */
export type DocumentStepKey =
  | 'invoice'
  | 'packing_list'
  | 'catch_certificates'
  | 'shipping_list'
  | 'draft_bl'
  | 'health_certificate'
  | 'certificate_of_origin'
  | 'review';

/** A business partner. Master data will later provide pickers; free text for now. */
export interface Party {
  name: string;
  country?: string;
}

export interface DocumentStep {
  key: DocumentStepKey;
  status: DocumentStatus;
}

export interface Project {
  id: string;
  reference: string;
  exporter: Party;
  producer: Party;
  consignee: Party;
  notifyParty: Party;
  freightForwarder: Party;
  shippingLine: Party;
  destination: string;
  loadingPort: string;
  dischargePort: string;
  /** ISO date (yyyy-MM-dd). ETD is an estimate. */
  etd: string;
  status: ProjectStatus;
  documentSteps: DocumentStep[];
  createdAt: string;
  updatedAt: string;
}

/** Payload accepted by `POST /api/projects`. */
export interface CreateProjectRequest {
  reference: string;
  exporter: Party;
  producer: Party;
  consignee: Party;
  notifyParty: Party;
  freightForwarder: Party;
  shippingLine: Party;
  destination: string;
  loadingPort: string;
  dischargePort: string;
  etd: string;
}

/** Partial update payload for `PATCH /api/projects/{id}`. */
export type UpdateProjectRequest = Partial<CreateProjectRequest> & {
  status?: ProjectStatus;
  documentSteps?: DocumentStep[];
};

export interface ProjectListQuery {
  search?: string;
  status?: ProjectStatus | 'ALL';
}

/** Lightweight projection used by the project table. */
export interface ProjectSummary {
  id: string;
  reference: string;
  customer: string;
  destination: string;
  etd: string;
  status: ProjectStatus;
  createdAt: string;
}

export interface DashboardStats {
  totalProjects: number;
  draftProjects: number;
  awaitingValidation: number;
  inProgress: number;
  completed: number;
}

export const DOCUMENT_STEP_LABELS: Record<DocumentStepKey, string> = {
  invoice: 'Invoice',
  packing_list: 'Packing List',
  catch_certificates: 'Catch Certificates',
  shipping_list: 'Shipping List',
  draft_bl: 'Draft BL',
  health_certificate: 'Health Certificate',
  certificate_of_origin: 'Certificate of Origin',
  review: 'Review & Validation',
};

export const DOCUMENT_STEP_DESCRIPTIONS: Record<DocumentStepKey, string> = {
  invoice: 'Commercial values, products and quantities.',
  packing_list: 'Cartons, net/gross weights, lots and containers.',
  catch_certificates: 'Vessels, catch dates and quantities per certificate.',
  shipping_list: 'HC annex aggregating vessel and lot catch amounts.',
  draft_bl: 'Vessel, voyage, container and seal details.',
  health_certificate: 'Regulatory health certificate draft.',
  certificate_of_origin: 'Origin declaration for the destination authority.',
  review: 'Cross-document validation and final pack.',
};

export const DOCUMENT_STEP_ORDER: DocumentStepKey[] = [
  'invoice',
  'packing_list',
  'catch_certificates',
  'shipping_list',
  'draft_bl',
  'health_certificate',
  'certificate_of_origin',
  'review',
];

export interface StatusMeta {
  label: string;
  tone: 'neutral' | 'info' | 'progress' | 'warning' | 'success' | 'danger';
}

export const PROJECT_STATUS_META: Record<ProjectStatus, StatusMeta> = {
  DRAFT: { label: 'Draft', tone: 'neutral' },
  IN_PROGRESS: { label: 'In progress', tone: 'progress' },
  READY_FOR_DRAFT_REVIEW: { label: 'Awaiting validation', tone: 'warning' },
  DRAFT_REVIEWED: { label: 'Draft reviewed', tone: 'info' },
  AWAITING_EXTERNAL_ISSUANCE: { label: 'Awaiting issuance', tone: 'info' },
  OFFICIAL_DOCUMENTS_RECEIVED: { label: 'Official docs received', tone: 'success' },
  COMPLETE: { label: 'Complete', tone: 'success' },
  ARCHIVED: { label: 'Archived', tone: 'neutral' },
};

export const DOCUMENT_STATUS_META: Record<DocumentStatus, StatusMeta> = {
  NOT_STARTED: { label: 'Not started', tone: 'neutral' },
  IN_PROGRESS: { label: 'In progress', tone: 'progress' },
  READY: { label: 'Ready', tone: 'info' },
  DRAFT: { label: 'Draft', tone: 'info' },
  STALE: { label: 'Stale', tone: 'warning' },
  APPROVED: { label: 'Approved', tone: 'success' },
  EXTERNAL_ISSUED: { label: 'Externally issued', tone: 'success' },
};

/** Statuses counted as "awaiting validation" on the dashboard. */
export const AWAITING_VALIDATION_STATUSES: ProjectStatus[] = [
  'READY_FOR_DRAFT_REVIEW',
  'DRAFT_REVIEWED',
];

export const COMPLETED_STATUSES: ProjectStatus[] = [
  'OFFICIAL_DOCUMENTS_RECEIVED',
  'COMPLETE',
  'ARCHIVED',
];
