/**
 * Frontend-only document workflow projection.
 *
 * Document forms and their statuses are not part of the frozen Phase 1 project
 * contract yet, so the workspace stepper derives a placeholder progression from
 * the project status. Replace with real per-form data once those endpoints land.
 */
import { ProjectStatus, StatusTone } from './project.model';

export type DocumentStatus =
  'NOT_STARTED' | 'IN_PROGRESS' | 'READY' | 'STALE' | 'APPROVED' | 'EXTERNAL_ISSUED';

export type DocumentStepKey =
  | 'invoice'
  | 'packing_list'
  | 'catch_certificates'
  | 'shipping_list'
  | 'bill_of_lading'
  | 'health_certificate'
  | 'certificate_of_origin'
  | 'review';

export interface DocumentStep {
  key: DocumentStepKey;
  status: DocumentStatus;
}

/** Contract `FormKey` values these steps will map to once forms exist. */
export type FormKey =
  | 'PROJECT_SETUP'
  | 'INVOICE'
  | 'PACKING_LIST'
  | 'CATCH_CERTIFICATES'
  | 'PROCESSING'
  | 'TRANSPORT'
  | 'HEALTH_CERTIFICATE'
  | 'BILL_OF_LADING'
  | 'CERTIFICATE_OF_ORIGIN'
  | 'SHIPPING_LIST';

export const STEP_FORM_KEY: Record<DocumentStepKey, FormKey | null> = {
  invoice: 'INVOICE',
  packing_list: 'PACKING_LIST',
  catch_certificates: 'CATCH_CERTIFICATES',
  shipping_list: 'SHIPPING_LIST',
  bill_of_lading: 'BILL_OF_LADING',
  health_certificate: 'HEALTH_CERTIFICATE',
  certificate_of_origin: 'CERTIFICATE_OF_ORIGIN',
  review: null,
};

export const DOCUMENT_STEP_ORDER: DocumentStepKey[] = [
  'invoice',
  'packing_list',
  'catch_certificates',
  'shipping_list',
  'bill_of_lading',
  'health_certificate',
  'certificate_of_origin',
  'review',
];

export const DOCUMENT_STEP_LABELS: Record<DocumentStepKey, string> = {
  invoice: 'Invoice',
  packing_list: 'Packing List',
  catch_certificates: 'Catch Certificates',
  shipping_list: 'Shipping List',
  bill_of_lading: 'Draft BL',
  health_certificate: 'Health Certificate',
  certificate_of_origin: 'Certificate of Origin',
  review: 'Review & Validation',
};

export const DOCUMENT_STEP_DESCRIPTIONS: Record<DocumentStepKey, string> = {
  invoice: 'Commercial values, products and quantities.',
  packing_list: 'Cartons, net/gross weights, lots and containers.',
  catch_certificates: 'Vessels, catch dates and quantities per certificate.',
  shipping_list: 'HC annex aggregating vessel and lot catch amounts.',
  bill_of_lading: 'Vessel, voyage, container and seal details.',
  health_certificate: 'Regulatory health certificate draft.',
  certificate_of_origin: 'Origin declaration for the destination authority.',
  review: 'Cross-document validation and final pack.',
};

export const DOCUMENT_STATUS_META: Record<DocumentStatus, { label: string; tone: StatusTone }> = {
  NOT_STARTED: { label: 'Not started', tone: 'neutral' },
  IN_PROGRESS: { label: 'In progress', tone: 'progress' },
  READY: { label: 'Ready', tone: 'info' },
  STALE: { label: 'Stale', tone: 'warning' },
  APPROVED: { label: 'Approved', tone: 'success' },
  EXTERNAL_ISSUED: { label: 'Externally issued', tone: 'success' },
};

export const COMPLETED_DOC_STATUSES: DocumentStatus[] = ['READY', 'APPROVED', 'EXTERNAL_ISSUED'];

type StatusMap = Partial<Record<DocumentStepKey, DocumentStatus>>;

const PROGRESS_BY_PROJECT_STATUS: Record<ProjectStatus, StatusMap> = {
  DRAFT_DATA: {},
  IN_PROGRESS: {
    invoice: 'APPROVED',
    packing_list: 'APPROVED',
    catch_certificates: 'IN_PROGRESS',
    shipping_list: 'NOT_STARTED',
  },
  READY_FOR_DRAFT_REVIEW: {
    invoice: 'APPROVED',
    packing_list: 'APPROVED',
    catch_certificates: 'READY',
    shipping_list: 'READY',
    bill_of_lading: 'READY',
    health_certificate: 'READY',
    certificate_of_origin: 'READY',
    review: 'IN_PROGRESS',
  },
  DRAFT_REVIEWED: {
    invoice: 'APPROVED',
    packing_list: 'APPROVED',
    catch_certificates: 'APPROVED',
    shipping_list: 'APPROVED',
    bill_of_lading: 'APPROVED',
    health_certificate: 'APPROVED',
    certificate_of_origin: 'APPROVED',
    review: 'IN_PROGRESS',
  },
  AWAITING_EXTERNAL_ISSUANCE: {
    invoice: 'APPROVED',
    packing_list: 'APPROVED',
    catch_certificates: 'APPROVED',
    shipping_list: 'APPROVED',
    bill_of_lading: 'APPROVED',
    health_certificate: 'EXTERNAL_ISSUED',
    certificate_of_origin: 'EXTERNAL_ISSUED',
    review: 'APPROVED',
  },
  OFFICIAL_DOCUMENTS_RECEIVED: {
    invoice: 'APPROVED',
    packing_list: 'APPROVED',
    catch_certificates: 'APPROVED',
    shipping_list: 'APPROVED',
    bill_of_lading: 'APPROVED',
    health_certificate: 'EXTERNAL_ISSUED',
    certificate_of_origin: 'EXTERNAL_ISSUED',
    review: 'APPROVED',
  },
  COMPLETE: {
    invoice: 'APPROVED',
    packing_list: 'APPROVED',
    catch_certificates: 'APPROVED',
    shipping_list: 'APPROVED',
    bill_of_lading: 'APPROVED',
    health_certificate: 'EXTERNAL_ISSUED',
    certificate_of_origin: 'EXTERNAL_ISSUED',
    review: 'APPROVED',
  },
  ARCHIVED: {
    invoice: 'APPROVED',
    packing_list: 'APPROVED',
    catch_certificates: 'APPROVED',
    shipping_list: 'APPROVED',
    bill_of_lading: 'APPROVED',
    health_certificate: 'EXTERNAL_ISSUED',
    certificate_of_origin: 'EXTERNAL_ISSUED',
    review: 'APPROVED',
  },
};

export function deriveDocumentSteps(status: ProjectStatus): DocumentStep[] {
  const map = PROGRESS_BY_PROJECT_STATUS[status] ?? {};
  return DOCUMENT_STEP_ORDER.map((key) => ({
    key,
    status: map[key] ?? 'NOT_STARTED',
  }));
}
