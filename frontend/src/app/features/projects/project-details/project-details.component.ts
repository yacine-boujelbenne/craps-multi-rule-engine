import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Subject, catchError, forkJoin, of, startWith, switchMap } from 'rxjs';
import {
  COMPLETED_DOC_STATUSES,
  DOCUMENT_STATUS_META,
  DOCUMENT_STEP_DESCRIPTIONS,
  DOCUMENT_STEP_LABELS,
  DocumentStepKey,
  deriveDocumentSteps,
} from '../../../core/models/document-step';
import { ProjectPermissions } from '../../../core/models/account.model';
import { PROJECT_STATUS_META, Project } from '../../../core/models/project.model';
import { ASSIGNMENT_SERVICE } from '../../../core/services/assignment.service';
import { AuthStore } from '../../../core/services/auth.store';
import { DirectoryStore } from '../../../core/services/directory.store';
import { NotificationService } from '../../../core/services/notification.service';
import { ProjectStore } from '../../../core/services/project.store';
import { formatDate, formatDateTime, initials, relativeToToday } from '../../../core/utils/format';
import { EmptyStateComponent } from '../../../shared/components/empty-state/empty-state.component';
import { IconComponent, IconName } from '../../../shared/components/icon/icon.component';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { PanelComponent } from '../../../shared/components/panel/panel.component';
import { StatusBadgeComponent } from '../../../shared/components/status-badge/status-badge.component';
import { CommercialPartiesComponent } from '../commercial-parties/commercial-parties.component';

@Component({
  selector: 'app-project-details',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterLink,
    IconComponent,
    PageHeaderComponent,
    PanelComponent,
    StatusBadgeComponent,
    EmptyStateComponent,
    CommercialPartiesComponent,
  ],
  templateUrl: './project-details.component.html',
  styleUrl: './project-details.component.scss',
})
export class ProjectDetailsComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly store = inject(ProjectStore);
  private readonly directory = inject(DirectoryStore);
  private readonly notifications = inject(NotificationService);
  private readonly auth = inject(AuthStore);
  private readonly assignments = inject(ASSIGNMENT_SERVICE);

  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly project = signal<Project | null>(null);
  protected readonly permissions = signal<ProjectPermissions | null>(null);

  /** Admins may always edit setup; operators need the PROJECT_SETUP grant. */
  protected readonly canEditSetup = computed(
    () =>
      this.auth.isAdmin() || (this.permissions()?.editableForms.includes('PROJECT_SETUP') ?? false),
  );

  protected readonly canManageAccess = computed(
    () => this.auth.isAdmin() || (this.permissions()?.canManageAssignments ?? false),
  );

  protected readonly workflowSubtitle = computed(() => {
    const editable = this.permissions()?.editableForms.length ?? 0;
    if (!this.auth.isAdmin() && editable === 0) {
      return 'Read-only access to this shipment. Forms are added in a later milestone.';
    }
    return 'Chronological steps. Forms are added in a later milestone.';
  });

  protected readonly stepLabels = DOCUMENT_STEP_LABELS;
  protected readonly stepDescriptions = DOCUMENT_STEP_DESCRIPTIONS;
  protected readonly docStatusMeta = DOCUMENT_STATUS_META;
  protected readonly projectStatusMeta = PROJECT_STATUS_META;

  protected readonly steps = computed(() => {
    const project = this.project();
    return project ? deriveDocumentSteps(project.status) : [];
  });

  protected readonly completedSteps = computed(
    () => this.steps().filter((step) => COMPLETED_DOC_STATUSES.includes(step.status)).length,
  );

  protected readonly progressPercent = computed(() => {
    const total = this.steps().length;
    return total === 0 ? 0 : Math.round((this.completedSteps() / total) * 100);
  });

  protected readonly consigneeName = computed(() => {
    const project = this.project();
    return project ? this.directory.organizationName(project.consigneeId) : '—';
  });

  protected readonly forwarderName = computed(() =>
    this.directory.organizationName(this.project()?.freightForwarderId),
  );

  protected readonly shippingLineName = computed(() =>
    this.directory.organizationName(this.project()?.shippingLineId),
  );

  protected readonly partners = computed(() => {
    const project = this.project();
    if (!project) {
      return [] as { label: string; icon: IconName; name: string }[];
    }
    return [
      {
        label: 'Exporter',
        icon: 'building' as IconName,
        name: this.directory.organizationName(project.exporterId),
      },
      {
        label: 'Producers',
        icon: 'factory' as IconName,
        name: this.directory.organizationNames(project.producerIds).join(', ') || '—',
      },
      {
        label: 'Consignee',
        icon: 'users' as IconName,
        name: this.directory.organizationName(project.consigneeId),
      },
      {
        label: 'Notify party',
        icon: 'bell' as IconName,
        name: this.directory.organizationName(project.notifyPartyId),
      },
      {
        label: 'Freight forwarder',
        icon: 'truck' as IconName,
        name: this.directory.organizationName(project.freightForwarderId),
      },
      {
        label: 'Shipping line',
        icon: 'anchor' as IconName,
        name: this.directory.organizationName(project.shippingLineId),
      },
    ];
  });

  private readonly reload$ = new Subject<void>();

  constructor() {
    this.directory.ensureLoaded();

    this.route.paramMap
      .pipe(
        switchMap((params) => {
          const id = params.get('id') ?? '';
          return this.reload$.pipe(
            startWith(undefined),
            switchMap(() => {
              if (!this.project()) {
                this.loading.set(true);
              }
              this.error.set(null);
              return forkJoin({
                project: this.store.getProject(id),
                permissions: this.assignments.permissions(id).pipe(catchError(() => of(null))),
              });
            }),
          );
        }),
        takeUntilDestroyed(),
      )
      .subscribe({
        next: ({ project, permissions }) => {
          this.project.set(project);
          this.permissions.set(permissions);
          this.loading.set(false);
        },
        error: () => {
          this.error.set('This project could not be loaded.');
          this.loading.set(false);
        },
      });
  }

  /** Refreshes the project after a shared-version write (e.g. commercial parties). */
  protected onPartiesSaved(): void {
    this.reload$.next();
  }

  /** Commercial parties and Project v2 share one version; refresh if they diverge. */
  protected onPartiesLoaded(version: number): void {
    const project = this.project();
    if (project && project.version !== version) {
      this.reload$.next();
    }
  }

  protected countryName(code: string | null | undefined): string {
    return this.directory.countryName(code);
  }

  protected portName(id: string | null | undefined): string {
    return this.directory.portName(id);
  }

  protected stepStatus(key: DocumentStepKey) {
    return this.steps().find((item) => item.key === key)?.status ?? 'NOT_STARTED';
  }

  protected isStepDone(key: DocumentStepKey): boolean {
    return COMPLETED_DOC_STATUSES.includes(this.stepStatus(key));
  }

  protected openStep(key: DocumentStepKey): void {
    this.notifications.info(
      `The ${this.stepLabels[key]} form is not available yet. It will be added in a later milestone.`,
    );
  }

  protected readonly formatDate = formatDate;
  protected readonly formatDateTime = formatDateTime;
  protected readonly relativeToToday = relativeToToday;
  protected readonly initials = initials;
}
