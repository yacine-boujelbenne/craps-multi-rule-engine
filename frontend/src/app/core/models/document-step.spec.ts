import { describe, expect, it } from 'vitest';
import { COMPLETED_DOC_STATUSES, DOCUMENT_STEP_ORDER, deriveDocumentSteps } from './document-step';

describe('deriveDocumentSteps', () => {
  it('returns every step for the initial draft status', () => {
    const steps = deriveDocumentSteps('DRAFT_DATA');
    expect(steps).toHaveLength(DOCUMENT_STEP_ORDER.length);
    expect(steps.every((step) => step.status === 'NOT_STARTED')).toBe(true);
  });

  it('marks all steps complete for a completed project', () => {
    const steps = deriveDocumentSteps('COMPLETE');
    expect(steps.every((step) => COMPLETED_DOC_STATUSES.includes(step.status))).toBe(true);
  });

  it('keeps the workflow in chronological order', () => {
    const steps = deriveDocumentSteps('IN_PROGRESS');
    expect(steps.map((step) => step.key)).toEqual(DOCUMENT_STEP_ORDER);
  });
});
