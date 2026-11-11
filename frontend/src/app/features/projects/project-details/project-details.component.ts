import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { switchMap } from 'rxjs';
import {
  DOCUMENT_STATUS_META,
  DOCUMENT_STEP_DESCRIPTIONS,
  DOCUMENT_STEP_LABELS,
  DOCUMENT_STEP_ORDER,
  DocumentStepKey,
  PROJECT_STATUS_META,
  Project,
} from '../../../core/models/project.model';
import { NotificationService } from '../../../core/services/notification.service';
import { ProjectStore } from '../../../core/services/project.store';
import { formatDate, formatDateTime, initials, relativeToToday } from '../../../core/utils/format';
import { EmptyStateComponent } from '../../../shared/components/empty-state/empty-state.component';
import { IconComponent, IconName } from '../../../shared/components/icon/icon.component';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { PanelComponent } from '../../../shared/components/panel/panel.component';
import { StatusBadgeComponent } from '../../../shared/components/status-badge/status-badge.component';

const COMPLETED_DOC_STATUSES = ['READY', 'APPROVED', 'EXTERNAL_ISSUED'];

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
  private readonly notifications = inject(NotificationService);

  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly project = signal<Project | null>(null);

  protected readonly steps = DOCUMENT_STEP_ORDER;
  protected readonly stepLabels = DOCUMENT_STEP_LABELS;
  protected readonly stepDescriptions = DOCUMENT_STEP_DESCRIPTIONS;
  protected readonly docStatusMeta = DOCUMENT_STATUS_META;
  protected readonly projectStatusMeta = PROJECT_STATUS_META;

  protected readonly completedSteps = computed(() => {
    const project = this.project();
    if (!project) {
      return 0;
    }
    return project.documentSteps.filter((step) => COMPLETED_DOC_STATUSES.includes(step.status))
      .length;
  });

  protected readonly progressPercent = computed(() =>
    Math.round((this.completedSteps() / this.steps.length) * 100),
  );

  protected readonly partners = computed(() => {
    const project = this.project();
    if (!project) {
      return [];
    }
    return [
      { label: 'Exporter', icon: 'building' as IconName, party: project.exporter },
      { label: 'Producer', icon: 'factory' as IconName, party: project.producer },
      { label: 'Consignee', icon: 'users' as IconName, party: project.consignee },
      { label: 'Notify party', icon: 'bell' as IconName, party: project.notifyParty },
      { label: 'Freight forwarder', icon: 'truck' as IconName, party: project.freightForwarder },
      { label: 'Shipping line', icon: 'anchor' as IconName, party: project.shippingLine },
    ];
  });

  constructor() {
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

  protected stepStatus(key: DocumentStepKey) {
    const step = this.project()?.documentSteps.find((item) => item.key === key);
    return step?.status ?? 'NOT_STARTED';
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
