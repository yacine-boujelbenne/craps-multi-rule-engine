import { describe, expect, it } from 'vitest';
import { formatDate, formatDateTime, initials, relativeToToday, suggestReference } from './format';
import { Project } from '../models/project.model';

describe('format utils', () => {
  it('formats ISO dates for display', () => {
    expect(formatDate('2026-11-11')).toBe('11 Nov 2026');
  });

  it('returns an em dash for empty or invalid values', () => {
    expect(formatDate(null)).toBe('—');
    expect(formatDate(undefined)).toBe('—');
    expect(formatDate('not-a-date')).toBe('—');
  });

  it('formats timestamps with time', () => {
    expect(formatDateTime('2026-11-11T09:30:00.000Z')).toMatch(/11 Nov 2026/);
  });

  it('describes dates relative to today', () => {
    const inThreeDays = new Date(Date.now() + 3 * 86_400_000).toISOString().slice(0, 10);
    expect(relativeToToday(inThreeDays)).toBe('in 3 days');
    expect(relativeToToday(new Date().toISOString().slice(0, 10))).toBe('today');
  });

  it('builds initials from a name', () => {
    expect(initials('Iberia Import S.L.')).toBe('II');
    expect(initials('Maersk')).toBe('M');
  });

  it('suggests the next reference for the given year', () => {
    const projects = [
      { reference: 'EXP-2026-0003' },
      { reference: 'EXP-2026-0007' },
      { reference: 'EXP-2025-0009' },
    ] as Project[];
    expect(suggestReference(projects, 2026)).toBe('EXP-2026-0008');
  });

  it('starts the sequence at 0001 when none exist', () => {
    expect(suggestReference([], 2026)).toBe('EXP-2026-0001');
  });
});
