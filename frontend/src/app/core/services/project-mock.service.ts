import { Injectable } from '@angular/core';
import { Observable, of, throwError } from 'rxjs';
import { delay } from 'rxjs/operators';
import { MOCK_PROJECTS } from '../data/mock-projects';
import {
  CreateProjectRequest,
  DOCUMENT_STEP_ORDER,
  DocumentStep,
  Project,
  ProjectListQuery,
  UpdateProjectRequest,
} from '../models/project.model';
import { ProjectService } from './project.service';

const STORAGE_KEY = 'exportdocs.mock.projects';
const LATENCY_MS = 320;

/**
 * In-memory implementation of {@link ProjectService}.
 *
 * Fully isolated from any real endpoint: it seeds from anonymized fixtures and
 * persists to localStorage so created projects survive a reload during
 * development. Replaced by the HTTP implementation once the API contract lands.
 */
@Injectable()
export class ProjectMockService implements ProjectService {
  private projects: Project[] = this.load();

  list(query?: ProjectListQuery): Observable<Project[]> {
    let items = [...this.projects];

    const search = query?.search?.trim().toLowerCase();
    if (search) {
      items = items.filter((project) =>
        [
          project.reference,
          project.consignee.name,
          project.exporter.name,
          project.destination,
        ].some((value) => value.toLowerCase().includes(search)),
      );
    }

    if (query?.status && query.status !== 'ALL') {
      items = items.filter((project) => project.status === query.status);
    }

    items.sort((a, b) => b.createdAt.localeCompare(a.createdAt));
    return of(items.map(clone)).pipe(delay(LATENCY_MS));
  }

  get(id: string): Observable<Project> {
    const project = this.projects.find((item) => item.id === id);
    if (!project) {
      return throwError(() => new Error(`Project ${id} not found`)).pipe(delay(LATENCY_MS));
    }
    return of(clone(project)).pipe(delay(LATENCY_MS));
  }

  create(input: CreateProjectRequest): Observable<Project> {
    const now = new Date().toISOString();
    const project: Project = {
      id: generateId(),
      ...clone(input),
      reference: input.reference || this.nextReference(),
      status: 'DRAFT',
      documentSteps: initialSteps(),
      createdAt: now,
      updatedAt: now,
    };
    this.projects = [project, ...this.projects];
    this.persist();
    return of(clone(project)).pipe(delay(LATENCY_MS));
  }

  update(id: string, patch: UpdateProjectRequest): Observable<Project> {
    const index = this.projects.findIndex((item) => item.id === id);
    if (index === -1) {
      return throwError(() => new Error(`Project ${id} not found`)).pipe(delay(LATENCY_MS));
    }
    const updated: Project = {
      ...this.projects[index],
      ...clone(patch),
      updatedAt: new Date().toISOString(),
    };
    this.projects = this.projects.map((item, i) => (i === index ? updated : item));
    this.persist();
    return of(clone(updated)).pipe(delay(LATENCY_MS));
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

function initialSteps(): DocumentStep[] {
  return DOCUMENT_STEP_ORDER.map((key) => ({ key, status: 'NOT_STARTED' as const }));
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
