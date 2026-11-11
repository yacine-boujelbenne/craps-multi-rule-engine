import {
  DOCUMENT_STEP_ORDER,
  DocumentStatus,
  DocumentStep,
  DocumentStepKey,
  Project,
  ProjectStatus,
} from '../models/project.model';

/**
 * Anonymized, fictional fixture data.
 *
 * Used only by the mock project service while the backend endpoints are being
 * implemented. Never place real customer data in the repository.
 */

const DAY_MS = 86_400_000;

function isoDate(offsetDays: number): string {
  return new Date(Date.now() + offsetDays * DAY_MS).toISOString().slice(0, 10);
}

function isoDateTime(offsetDays: number): string {
  return new Date(Date.now() + offsetDays * DAY_MS).toISOString();
}

function steps(overrides: Partial<Record<DocumentStepKey, DocumentStatus>> = {}): DocumentStep[] {
  return DOCUMENT_STEP_ORDER.map((key) => ({
    key,
    status: overrides[key] ?? 'NOT_STARTED',
  }));
}

const STEPS_DRAFT = steps();

const STEPS_IN_PROGRESS = steps({
  invoice: 'APPROVED',
  packing_list: 'APPROVED',
  catch_certificates: 'DRAFT',
  shipping_list: 'IN_PROGRESS',
});

const STEPS_AWAITING_VALIDATION = steps({
  invoice: 'APPROVED',
  packing_list: 'APPROVED',
  catch_certificates: 'READY',
  shipping_list: 'READY',
  draft_bl: 'READY',
  health_certificate: 'READY',
  certificate_of_origin: 'READY',
  review: 'IN_PROGRESS',
});

const STEPS_STALE = steps({
  invoice: 'APPROVED',
  packing_list: 'STALE',
  catch_certificates: 'APPROVED',
  shipping_list: 'DRAFT',
});

const STEPS_NEARLY_DONE = steps({
  invoice: 'APPROVED',
  packing_list: 'APPROVED',
  catch_certificates: 'APPROVED',
  shipping_list: 'APPROVED',
  draft_bl: 'APPROVED',
  health_certificate: 'EXTERNAL_ISSUED',
  certificate_of_origin: 'EXTERNAL_ISSUED',
  review: 'APPROVED',
});

export const MOCK_PROJECTS: Project[] = [
  {
    id: '11111111-1111-4111-8111-000000000001',
    reference: 'EXP-2026-0001',
    exporter: { name: 'Atlantic Blue Seafood Co.', country: 'Morocco' },
    producer: { name: 'Nordic Catch Processing', country: 'Morocco' },
    consignee: { name: 'Iberia Import S.L.', country: 'Spain' },
    notifyParty: { name: 'Iberia Import S.L.', country: 'Spain' },
    freightForwarder: { name: 'TransGlobal Logistics', country: 'Morocco' },
    shippingLine: { name: 'Maersk Line' },
    destination: 'Spain — Vigo',
    loadingPort: 'Tanger Med',
    dischargePort: 'Vigo',
    etd: isoDate(6),
    status: 'IN_PROGRESS',
    documentSteps: STEPS_IN_PROGRESS,
    createdAt: isoDateTime(-12),
    updatedAt: isoDateTime(-1),
  },
  {
    id: '11111111-1111-4111-8111-000000000002',
    reference: 'EXP-2026-0002',
    exporter: { name: 'Atlantic Blue Seafood Co.', country: 'Morocco' },
    producer: { name: 'Nordic Catch Processing', country: 'Morocco' },
    consignee: { name: 'Mediterraneo Food Group', country: 'Italy' },
    notifyParty: { name: 'Adriatic Customs Broker', country: 'Italy' },
    freightForwarder: { name: 'TransGlobal Logistics', country: 'Morocco' },
    shippingLine: { name: 'CMA CGM' },
    destination: 'Italy — Genoa',
    loadingPort: 'Tanger Med',
    dischargePort: 'Genoa',
    etd: isoDate(2),
    status: 'READY_FOR_DRAFT_REVIEW',
    documentSteps: STEPS_AWAITING_VALIDATION,
    createdAt: isoDateTime(-20),
    updatedAt: isoDateTime(-2),
  },
  {
    id: '11111111-1111-4111-8111-000000000003',
    reference: 'EXP-2026-0003',
    exporter: { name: 'Atlantic Blue Seafood Co.', country: 'Morocco' },
    producer: { name: 'Sahara Fisheries', country: 'Morocco' },
    consignee: { name: 'Tokyo Marine Foods K.K.', country: 'Japan' },
    notifyParty: { name: 'Tokyo Marine Foods K.K.', country: 'Japan' },
    freightForwarder: { name: 'Nippon Cargo Services', country: 'Japan' },
    shippingLine: { name: 'ONE' },
    destination: 'Japan — Yokohama',
    loadingPort: 'Tanger Med',
    dischargePort: 'Yokohama',
    etd: isoDate(18),
    status: 'DRAFT',
    documentSteps: STEPS_DRAFT,
    createdAt: isoDateTime(-3),
    updatedAt: isoDateTime(-3),
  },
  {
    id: '11111111-1111-4111-8111-000000000004',
    reference: 'EXP-2026-0004',
    exporter: { name: 'Atlantic Blue Seafood Co.', country: 'Morocco' },
    producer: { name: 'Nordic Catch Processing', country: 'Morocco' },
    consignee: { name: 'Hanseatic Seafood GmbH', country: 'Germany' },
    notifyParty: { name: 'Hanseatic Seafood GmbH', country: 'Germany' },
    freightForwarder: { name: 'EuroFreight AG', country: 'Germany' },
    shippingLine: { name: 'Hapag-Lloyd' },
    destination: 'Germany — Hamburg',
    loadingPort: 'Agadir',
    dischargePort: 'Hamburg',
    etd: isoDate(-4),
    status: 'DRAFT_REVIEWED',
    documentSteps: STEPS_STALE,
    createdAt: isoDateTime(-34),
    updatedAt: isoDateTime(-5),
  },
  {
    id: '11111111-1111-4111-8111-000000000005',
    reference: 'EXP-2026-0005',
    exporter: { name: 'Atlantic Blue Seafood Co.', country: 'Morocco' },
    producer: { name: 'Sahara Fisheries', country: 'Morocco' },
    consignee: { name: 'Pacific Rim Trading Ltd.', country: 'United States' },
    notifyParty: { name: 'Pacific Rim Trading Ltd.', country: 'United States' },
    freightForwarder: { name: 'OceanLink Forwarding', country: 'United States' },
    shippingLine: { name: 'MSC' },
    destination: 'United States — Newark',
    loadingPort: 'Casablanca',
    dischargePort: 'Newark',
    etd: isoDate(9),
    status: 'AWAITING_EXTERNAL_ISSUANCE',
    documentSteps: STEPS_NEARLY_DONE,
    createdAt: isoDateTime(-45),
    updatedAt: isoDateTime(-3),
  },
  {
    id: '11111111-1111-4111-8111-000000000006',
    reference: 'EXP-2026-0006',
    exporter: { name: 'Atlantic Blue Seafood Co.', country: 'Morocco' },
    producer: { name: 'Nordic Catch Processing', country: 'Morocco' },
    consignee: { name: 'Iberia Import S.L.', country: 'Spain' },
    notifyParty: { name: 'Barcelona Fresh Logistics', country: 'Spain' },
    freightForwarder: { name: 'TransGlobal Logistics', country: 'Morocco' },
    shippingLine: { name: 'Maersk Line' },
    destination: 'Spain — Barcelona',
    loadingPort: 'Tanger Med',
    dischargePort: 'Barcelona',
    etd: isoDate(-15),
    status: 'COMPLETE',
    documentSteps: steps({
      invoice: 'APPROVED',
      packing_list: 'APPROVED',
      catch_certificates: 'APPROVED',
      shipping_list: 'APPROVED',
      draft_bl: 'APPROVED',
      health_certificate: 'EXTERNAL_ISSUED',
      certificate_of_origin: 'EXTERNAL_ISSUED',
      review: 'APPROVED',
    }),
    createdAt: isoDateTime(-70),
    updatedAt: isoDateTime(-18),
  },
  {
    id: '11111111-1111-4111-8111-000000000007',
    reference: 'EXP-2026-0007',
    exporter: { name: 'Atlantic Blue Seafood Co.', country: 'Morocco' },
    producer: { name: 'Sahara Fisheries', country: 'Morocco' },
    consignee: { name: 'Lyon Gastronomie SARL', country: 'France' },
    notifyParty: { name: 'Lyon Gastronomie SARL', country: 'France' },
    freightForwarder: { name: 'EuroFreight AG', country: 'Germany' },
    shippingLine: { name: 'CMA CGM' },
    destination: 'France — Le Havre',
    loadingPort: 'Agadir',
    dischargePort: 'Le Havre',
    etd: isoDate(4),
    status: 'IN_PROGRESS',
    documentSteps: STEPS_IN_PROGRESS,
    createdAt: isoDateTime(-9),
    updatedAt: isoDateTime(-1),
  },
  {
    id: '11111111-1111-4111-8111-000000000008',
    reference: 'EXP-2026-0008',
    exporter: { name: 'Atlantic Blue Seafood Co.', country: 'Morocco' },
    producer: { name: 'Nordic Catch Processing', country: 'Morocco' },
    consignee: { name: 'Gulf Delicacies LLC', country: 'United Arab Emirates' },
    notifyParty: { name: 'Jebel Ali Clearing Co.', country: 'United Arab Emirates' },
    freightForwarder: { name: 'Gulf Freight Partners', country: 'United Arab Emirates' },
    shippingLine: { name: 'MSC' },
    destination: 'UAE — Jebel Ali',
    loadingPort: 'Casablanca',
    dischargePort: 'Jebel Ali',
    etd: isoDate(24),
    status: 'DRAFT',
    documentSteps: STEPS_DRAFT,
    createdAt: isoDateTime(-1),
    updatedAt: isoDateTime(-1),
  },
  {
    id: '11111111-1111-4111-8111-000000000009',
    reference: 'EXP-2026-0009',
    exporter: { name: 'Atlantic Blue Seafood Co.', country: 'Morocco' },
    producer: { name: 'Sahara Fisheries', country: 'Morocco' },
    consignee: { name: 'Iberia Import S.L.', country: 'Spain' },
    notifyParty: { name: 'Iberia Import S.L.', country: 'Spain' },
    freightForwarder: { name: 'TransGlobal Logistics', country: 'Morocco' },
    shippingLine: { name: 'Hapag-Lloyd' },
    destination: 'Spain — Vigo',
    loadingPort: 'Tanger Med',
    dischargePort: 'Vigo',
    etd: isoDate(31),
    status: 'READY_FOR_DRAFT_REVIEW',
    documentSteps: STEPS_AWAITING_VALIDATION,
    createdAt: isoDateTime(-6),
    updatedAt: isoDateTime(-1),
  },
  {
    id: '11111111-1111-4111-8111-00000000000a',
    reference: 'EXP-2026-0010',
    exporter: { name: 'Atlantic Blue Seafood Co.', country: 'Morocco' },
    producer: { name: 'Nordic Catch Processing', country: 'Morocco' },
    consignee: { name: 'Mediterraneo Food Group', country: 'Italy' },
    notifyParty: { name: 'Adriatic Customs Broker', country: 'Italy' },
    freightForwarder: { name: 'EuroFreight AG', country: 'Germany' },
    shippingLine: { name: 'ONE' },
    destination: 'Italy — Genoa',
    loadingPort: 'Agadir',
    dischargePort: 'Genoa',
    etd: isoDate(-30),
    status: 'ARCHIVED',
    documentSteps: STEPS_NEARLY_DONE,
    createdAt: isoDateTime(-95),
    updatedAt: isoDateTime(-40),
  },
];
