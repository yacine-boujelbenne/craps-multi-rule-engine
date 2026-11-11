import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, forkJoin, map, tap } from 'rxjs';
import {
  AWAITING_VALIDATION_STATUSES,
  COMPLETED_STATUSES,
  CreateProjectRequest,
  DashboardStats,
  DRAFT_STATUSES,
  IN_PROGRESS_STATUSES,
  Project,
  ProjectListQuery,
  ProjectPage,
  PROJECT_STATUSES,
  ProjectStatus,
  UpdateProjectRequest,
} from '../models/project.model';
import { NotificationService } from './notification.service';
import { PROJECT_SERVICE } from './project.service';

const EMPTY_PAGE: ProjectPage = { items: [], page: 0, size: 20, totalElements: 0, totalPages: 0 };
const RECENT_LIMIT = 5;
const UPCOMING_LIMIT = 5;
const UPCOMING_SCAN_SIZE = 100;

/**
 * Signal-based state container shared by the dashboard, project list and
 * workspace. The contract has no stats endpoint, so dashboard figures come from
 * per-status counts (`list({ status, size: 1 }).totalElements`).
 */
@Injectable({ providedIn: 'root' })
export class ProjectStore {
  private readonly service = inject(PROJECT_SERVICE);
  private readonly notifications = inject(NotificationService);

  private readonly _page = signal<ProjectPage>(EMPTY_PAGE);
  private readonly _query = signal<ProjectListQuery>({ page: 0, size: EMPTY_PAGE.size });
  private readonly _counts = signal<Partial<Record<ProjectStatus, number>>>({});
  private readonly _recent = signal<Project[]>([]);
  private readonly _upcoming = signal<Project[]>([]);
  private readonly _loading = signal(false);
  private readonly _error = signal<string | null>(null);
  private readonly _loaded = signal(false);

  readonly page = this._page.asReadonly();
  readonly query = this._query.asReadonly();
  readonly loading = this._loading.asReadonly();
  readonly error = this._error.asReadonly();
  readonly loaded = this._loaded.asReadonly();
  readonly recentProjects = this._recent.asReadonly();
  readonly upcomingDepartures = this._upcoming.asReadonly();

  readonly projects = computed<Project[]>(() => this._page().items);
  readonly totalElements = computed(() => this._page().totalElements);
  readonly totalPages = computed(() => this._page().totalPages);

  readonly stats = computed<DashboardStats>(() => {
    const counts = this._counts();
    const sum = (statuses: ProjectStatus[]) =>
      statuses.reduce((total, status) => total + (counts[status] ?? 0), 0);
    return {
      totalProjects: sum(PROJECT_STATUSES),
      draftProjects: sum(DRAFT_STATUSES),
      inProgress: sum(IN_PROGRESS_STATUSES),
      awaitingValidation: sum(AWAITING_VALIDATION_STATUSES),
      completed: sum(COMPLETED_STATUSES),
    };
  });

  loadPage(query: ProjectListQuery = {}): void {
    const next: ProjectListQuery = { page: 0, size: EMPTY_PAGE.size, ...query };
    this._query.set(next);
    this._loading.set(true);
    this._error.set(null);
    this.service.list(next).subscribe({
      next: (page) => {
        this._page.set(page);
        this._loading.set(false);
      },
      error: () => {
        this._error.set('Unable to load projects. Please try again.');
        this._loading.set(false);
      },
    });
  }

  loadDashboard(force = false): void {
    if (this._loaded() && !force) {
      return;
    }
    this._loading.set(true);
    this._error.set(null);

    const counts$ = PROJECT_STATUSES.map((status) =>
      this.service
        .list({ status, size: 1 })
        .pipe(map((page) => [status, page.totalElements] as const)),
    );
    const recent$ = this.service.list({ page: 0, size: RECENT_LIMIT });
    const upcoming$ = this.service.list({ page: 0, size: UPCOMING_SCAN_SIZE });

    forkJoin({ counts: forkJoin(counts$), recent: recent$, upcoming: upcoming$ }).subscribe({
      next: ({ counts, recent, upcoming }) => {
        this._counts.set(Object.fromEntries(counts) as Partial<Record<ProjectStatus, number>>);
        this._recent.set(recent.items);
        this._upcoming.set(upcomingDepartures(upcoming.items));
        this._loaded.set(true);
        this._loading.set(false);
      },
      error: () => {
        this._error.set('Unable to load dashboard data. Please try again.');
        this._loading.set(false);
      },
    });
  }

  getProject(id: string): Observable<Project> {
    return this.service.get(id).pipe(
      tap((project) => {
        this._page.update((page) => ({
          ...page,
          items: page.items.some((item) => item.id === id)
            ? page.items.map((item) => (item.id === id ? project : item))
            : page.items,
        }));
        this._recent.update((projects) =>
          projects.some((item) => item.id === id)
            ? projects.map((item) => (item.id === id ? project : item))
            : projects,
        );
      }),
    );
  }

  createProject(input: CreateProjectRequest): Observable<Project> {
    return this.service.create(input).pipe(
      tap((project) => {
        this._page.update((page) => ({
          ...page,
          items: [project, ...page.items],
          totalElements: page.totalElements + 1,
        }));
        this.notifications.success(`Project ${project.reference} created successfully.`);
      }),
    );
  }

  updateProject(id: string, patch: UpdateProjectRequest): Observable<Project> {
    return this.service.update(id, patch).pipe(
      tap((project) => {
        const replace = (projects: Project[]) =>
          projects.map((item) => (item.id === id ? project : item));
        this._page.update((page) => ({ ...page, items: replace(page.items) }));
        this._recent.update(replace);
        this._upcoming.update(replace);
      }),
    );
  }
}

function upcomingDepartures(projects: Project[]): Project[] {
  const today = new Date().toISOString().slice(0, 10);
  return projects
    .filter(
      (project) =>
        project.expectedDepartureDate !== null &&
        project.expectedDepartureDate >= today &&
        project.status !== 'ARCHIVED',
    )
    .sort((a, b) => a.expectedDepartureDate!.localeCompare(b.expectedDepartureDate!))
    .slice(0, UPCOMING_LIMIT);
}
