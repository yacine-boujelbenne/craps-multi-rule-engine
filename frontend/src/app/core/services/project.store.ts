import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import {
  AWAITING_VALIDATION_STATUSES,
  COMPLETED_STATUSES,
  CreateProjectRequest,
  DashboardStats,
  Project,
  ProjectListQuery,
  UpdateProjectRequest,
} from '../models/project.model';
import { NotificationService } from './notification.service';
import { PROJECT_SERVICE } from './project.service';

/**
 * Lightweight signal-based state container shared by the dashboard, project
 * list and project workspace. Keeps a single source of truth for loaded
 * projects without pulling in a state-management library.
 */
@Injectable({ providedIn: 'root' })
export class ProjectStore {
  private readonly service = inject(PROJECT_SERVICE);
  private readonly notifications = inject(NotificationService);

  private readonly _projects = signal<Project[]>([]);
  private readonly _loading = signal(false);
  private readonly _error = signal<string | null>(null);
  private readonly _loaded = signal(false);

  readonly projects = this._projects.asReadonly();
  readonly loading = this._loading.asReadonly();
  readonly error = this._error.asReadonly();
  readonly loaded = this._loaded.asReadonly();

  readonly stats = computed<DashboardStats>(() => {
    const projects = this._projects();
    return {
      totalProjects: projects.length,
      draftProjects: projects.filter((project) => project.status === 'DRAFT').length,
      awaitingValidation: projects.filter((project) =>
        AWAITING_VALIDATION_STATUSES.includes(project.status),
      ).length,
      inProgress: projects.filter((project) => project.status === 'IN_PROGRESS').length,
      completed: projects.filter((project) => COMPLETED_STATUSES.includes(project.status)).length,
    };
  });

  readonly recentProjects = computed<Project[]>(() =>
    [...this._projects()].sort((a, b) => b.createdAt.localeCompare(a.createdAt)).slice(0, 5),
  );

  readonly upcomingDepartures = computed<Project[]>(() => {
    const today = new Date().toISOString().slice(0, 10);
    return this._projects()
      .filter((project) => project.etd >= today && project.status !== 'ARCHIVED')
      .sort((a, b) => a.etd.localeCompare(b.etd))
      .slice(0, 5);
  });

  loadProjects(query?: ProjectListQuery, force = false): void {
    if (this._loaded() && !force && !query) {
      return;
    }
    this._loading.set(true);
    this._error.set(null);
    this.service.list(query).subscribe({
      next: (projects) => {
        this._projects.set(projects);
        this._loaded.set(true);
        this._loading.set(false);
      },
      error: () => {
        this._error.set('Unable to load projects. Please try again.');
        this._loading.set(false);
      },
    });
  }

  getProject(id: string): Observable<Project> {
    return this.service.get(id).pipe(
      tap((project) => {
        this._projects.update((projects) =>
          projects.some((item) => item.id === id)
            ? projects.map((item) => (item.id === id ? project : item))
            : [project, ...projects],
        );
      }),
    );
  }

  createProject(input: CreateProjectRequest): Observable<Project> {
    return this.service.create(input).pipe(
      tap((project) => {
        this._projects.update((projects) => [project, ...projects]);
        this.notifications.success(`Project ${project.reference} created successfully.`);
      }),
    );
  }

  updateProject(id: string, patch: UpdateProjectRequest): Observable<Project> {
    return this.service.update(id, patch).pipe(
      tap((project) => {
        this._projects.update((projects) =>
          projects.map((item) => (item.id === id ? project : item)),
        );
      }),
    );
  }
}
