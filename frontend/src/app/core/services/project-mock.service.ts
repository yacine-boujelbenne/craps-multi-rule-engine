import { InjectionToken, Inject, Injectable, Optional } from '@angular/core';
import { Observable, of, throwError } from 'rxjs';
import { delay } from 'rxjs/operators';
import { MOCK_PROJECTS } from '../data/mock-projects';
import {
  ApiProblemError,
  CreateProjectRequest,
  normalizeReference,
  Project,
  ProjectListQuery,
  ProjectPage,
  UpdateProjectRequest,
} from '../models/project.model';
import { ProjectService } from './project.service';

const STORAGE_KEY = 'exportdocs.mock.projects';
export const DEFAULT_MOCK_LATENCY_MS = 320;
export const MOCK_LATENCY_MS = new InjectionToken<number>('MOCK_LATENCY_MS');

/** Fields a PATCH may change (presence-aware; omitted means unchanged). */
const PATCHABLE_FIELDS = [
  'reference',
  'exporterId',
  'producerIds',
  'consigneeId',
  'notifyPartyId',
  'freightForwarderId',
  'shippingLineId',
  'destinationCountryCode',
  'portOfLoadingId',
  'portOfDischargeId',
  'expectedDepartureDate',
  'notes',
] as const;

/**
 * Fixture-backed {@link ProjectService} that mirrors the frozen contract:
 * zero-based pagination, status + literal reference-prefix filtering, required
 * `expectedVersion` on PATCH, and RFC 9457 Problem Details errors.
 *
 * Isolated from any real endpoint; persists to localStorage so created projects
 * survive a reload during development.
 */
@Injectable()
export class ProjectMockService implements ProjectService {
  private projects: Project[] = this.load();
  private readonly latency: number;

  constructor(@Optional() @Inject(MOCK_LATENCY_MS) latency?: number) {
    this.latency = latency ?? DEFAULT_MOCK_LATENCY_MS;
  }

  list(query: ProjectListQuery = {}): Observable<ProjectPage> {
    const page = Math.max(0, query.page ?? 0);
    const size = Math.min(100, Math.max(1, query.size ?? 20));

    let items = [...this.projects];
    if (query.status) {
      items = items.filter((project) => project.status === query.status);
    }
    const q = query.q?.trim();
    if (q) {
      const needle = q.toLowerCase();
      items = items.filter((project) => project.reference.toLowerCase().startsWith(needle));
    }

    items.sort((a, b) => b.createdAt.localeCompare(a.createdAt) || b.id.localeCompare(a.id));

    const totalElements = items.length;
    const totalPages = totalElements === 0 ? 0 : Math.ceil(totalElements / size);
    const start = page * size;

    const result: ProjectPage = {
      items: items.slice(start, start + size).map(clone),
      page,
      size,
      totalElements,
      totalPages,
    };
    return of(result).pipe(delay(this.latency));
  }

  get(id: string): Observable<Project> {
    const project = this.projects.find((item) => item.id === id);
    if (!project) {
      return this.fail(404, 'Project not found', `No project with id ${id}.`);
    }
    return of(clone(project)).pipe(delay(this.latency));
  }

  create(input: CreateProjectRequest): Observable<Project> {
    const reference = normalizeReference(input.reference);
    if (this.projects.some((project) => project.reference === reference)) {
      return this.fail(409, 'Duplicate reference', `Reference ${reference} already exists.`);
    }

    const now = new Date().toISOString();
    const project: Project = {
      id: generateId(),
      version: 0,
      status: 'DRAFT_DATA',
      reference,
      exporterId: input.exporterId ?? null,
      producerIds: [...(input.producerIds ?? [])],
      consigneeId: input.consigneeId ?? null,
      notifyPartyId: input.notifyPartyId ?? null,
      freightForwarderId: input.freightForwarderId ?? null,
      shippingLineId: input.shippingLineId ?? null,
      destinationCountryCode: input.destinationCountryCode ?? null,
      portOfLoadingId: input.portOfLoadingId ?? null,
      portOfDischargeId: input.portOfDischargeId ?? null,
      expectedDepartureDate: input.expectedDepartureDate ?? null,
      notes: input.notes ?? null,
      createdAt: now,
      updatedAt: now,
    };

    this.projects = [project, ...this.projects];
    this.persist();
    return of(clone(project)).pipe(delay(this.latency));
  }

  update(id: string, patch: UpdateProjectRequest): Observable<Project> {
    const current = this.projects.find((item) => item.id === id);
    if (!current) {
      return this.fail(404, 'Project not found', `No project with id ${id}.`);
    }
    if (patch.expectedVersion !== current.version) {
      return this.fail(
        409,
        'Stale version',
        `Expected version ${patch.expectedVersion} but the project is at ${current.version}.`,
      );
    }

    const next: Project = { ...current };
    for (const field of PATCHABLE_FIELDS) {
      if (field in patch) {
        (next as Record<string, unknown>)[field] = patch[field as keyof UpdateProjectRequest];
      }
    }
    if (patch.reference !== undefined) {
      next.reference = normalizeReference(patch.reference);
      const duplicate = this.projects.some(
        (project) => project.id !== id && project.reference === next.reference,
      );
      if (duplicate) {
        return this.fail(409, 'Duplicate reference', `Reference ${next.reference} already exists.`);
      }
    }

    if (!hasEffectiveChange(current, next)) {
      return of(clone(current)).pipe(delay(this.latency));
    }

    next.version = current.version + 1;
    next.updatedAt = new Date().toISOString();
    this.projects = this.projects.map((item) => (item.id === id ? next : item));
    this.persist();
    return of(clone(next)).pipe(delay(this.latency));
  }

  /** Suggests the next `EXP-YYYY-NNNN` reference. */
  nextReference(year = new Date().getFullYear()): string {
    const prefix = `EXP-${year}-`;
    const highest = this.projects
      .filter((project) => project.reference.startsWith(prefix))
      .map((project) => Number.parseInt(project.reference.slice(prefix.length), 10))
      .filter((value) => Number.isFinite(value))
      .reduce((max, value) => Math.max(max, value), 0);
    return `${prefix}${String(highest + 1).padStart(4, '0')}`;
  }

  private fail(status: number, title: string, detail: string): Observable<never> {
    return throwError(
      () => new ApiProblemError(status, { type: 'about:blank', title, status, detail }),
    ).pipe(delay(this.latency));
  }

  private load(): Project[] {
    const stored = safeRead();
    if (stored) {
      try {
        const parsed = JSON.parse(stored) as Project[];
        if (Array.isArray(parsed) && parsed.length > 0) {
          return parsed;
        }
      } catch {
        // fall through to fixtures
      }
    }
    return MOCK_PROJECTS.map(clone);
  }

  private persist(): void {
    safeWrite(JSON.stringify(this.projects));
  }
}

function hasEffectiveChange(current: Project, next: Project): boolean {
  return PATCHABLE_FIELDS.some((field) => {
    const a = JSON.stringify(current[field]);
    const b = JSON.stringify(next[field]);
    return a !== b;
  });
}

function clone<T>(value: T): T {
  return structuredClone(value);
}

function generateId(): string {
  if (typeof crypto !== 'undefined' && 'randomUUID' in crypto) {
    return crypto.randomUUID();
  }
  return `id-${Math.random().toString(36).slice(2)}-${Date.now()}`;
}

function safeRead(): string | null {
  try {
    return typeof localStorage === 'undefined' ? null : localStorage.getItem(STORAGE_KEY);
  } catch {
    return null;
  }
}

function safeWrite(value: string): void {
  try {
    if (typeof localStorage !== 'undefined') {
      localStorage.setItem(STORAGE_KEY, value);
    }
  } catch {
    // ignore storage failures (private mode, quota, SSR)
  }
}
