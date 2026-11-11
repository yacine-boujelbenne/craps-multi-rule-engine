import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { switchMap } from 'rxjs';
import {
  COMPLETED_DOC_STATUSES,
  DOCUMENT_STATUS_META,
  DOCUMENT_STEP_DESCRIPTIONS,
  DOCUMENT_STEP_LABELS,
  DocumentStepKey,
  deriveDocumentSteps,
} from '../../../core/models/document-step';
import { PROJECT_STATUS_META, Project } from '../../../core/models/project.model';
import { DirectoryStore } from '../../../core/services/directory.store';
import { NotificationService } from '../../../core/services/notification.service';
import { ProjectStore } from '../../../core/services/project.store';
import { formatDate, formatDateTime, initials, relativeToToday } from '../../../core/utils/format';
import { EmptyStateComponent } from '../../../shared/components/empty-state/empty-state.component';
import { IconComponent, IconName } from '../../../shared/components/icon/icon.component';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { PanelComponent } from '../../../shared/components/panel/panel.component';
import { StatusBadgeComponent } from '../../../shared/components/status-badge/status-badge.component';

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
  ],
  templateUrl: './project-details.component.html',
  styleUrl: './project-details.component.scss',
})
export class ProjectDetailsComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly store = inject(ProjectStore);
  private readonly directory = inject(DirectoryStore);
  private readonly notifications = inject(NotificationService);

  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly project = signal<Project | null>(null);

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

  constructor() {
    this.directory.ensureLoaded();

    this.route.paramMap
      .pipe(
        switchMap((params) => {
          this.loading.set(true);
          this.error.set(null);
          return this.store.getProject(params.get('id') ?? '');
        }),
        takeUntilDestroyed(),
      )
      .subscribe({
        next: (project) => {
          this.project.set(project);
          this.loading.set(false);
        },
        error: () => {
          this.error.set('This project could not be loaded.');
          this.loading.set(false);
        },
      });
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
