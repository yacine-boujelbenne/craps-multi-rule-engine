import { Project } from '../models/project.model';

const DATE_FORMATTER = new Intl.DateTimeFormat('en-GB', {
  day: '2-digit',
  month: 'short',
  year: 'numeric',
});

const DATE_TIME_FORMATTER = new Intl.DateTimeFormat('en-GB', {
  day: '2-digit',
  month: 'short',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
});

/** Formats an ISO date (yyyy-MM-dd) or timestamp for display. */
export function formatDate(value: string | undefined | null): string {
  if (!value) {
    return '—';
  }
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? '—' : DATE_FORMATTER.format(date);
}

export function formatDateTime(value: string | undefined | null): string {
  if (!value) {
    return '—';
  }
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? '—' : DATE_TIME_FORMATTER.format(date);
}

/** Human-friendly relative description of a date, e.g. "in 6 days", "3 days ago". */
export function relativeToToday(value: string | undefined | null): string {
  if (!value) {
    return '';
  }
  const target = new Date(value);
  if (Number.isNaN(target.getTime())) {
    return '';
  }
  const startOfToday = new Date();
  startOfToday.setHours(0, 0, 0, 0);
  const startOfTarget = new Date(target);
  startOfTarget.setHours(0, 0, 0, 0);

  const days = Math.round((startOfTarget.getTime() - startOfToday.getTime()) / 86_400_000);

  if (days === 0) return 'today';
  if (days === 1) return 'tomorrow';
  if (days === -1) return 'yesterday';
  if (days > 0) return `in ${days} days`;
  return `${Math.abs(days)} days ago`;
}

export function todayIso(): string {
  return new Date().toISOString().slice(0, 10);
}

/** Suggests the next `EXP-YYYY-NNNN` reference from existing projects. */
export function suggestReference(projects: Project[], year = new Date().getFullYear()): string {
  const prefix = `EXP-${year}-`;
  const highest = projects
    .map((project) => project.reference)
    .filter((reference) => reference.startsWith(prefix))
    .map((reference) => Number.parseInt(reference.slice(prefix.length), 10))
    .filter((value) => Number.isFinite(value))
    .reduce((max, value) => Math.max(max, value), 0);
  return `${prefix}${String(highest + 1).padStart(4, '0')}`;
}

export function initials(name: string): string {
  return name
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase() ?? '')
    .join('');
}
