import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  OnInit,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { PROJECT_STATUS_META, Project, ProjectStatus } from '../../../core/models/project.model';
import { ProjectStore } from '../../../core/services/project.store';
import { formatDate } from '../../../core/utils/format';
import { EmptyStateComponent } from '../../../shared/components/empty-state/empty-state.component';
import { IconComponent } from '../../../shared/components/icon/icon.component';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { StatusBadgeComponent } from '../../../shared/components/status-badge/status-badge.component';

@Component({
  selector: 'app-project-list',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    FormsModule,
    RouterLink,
    IconComponent,
    PageHeaderComponent,
    StatusBadgeComponent,
    EmptyStateComponent,
  ],
  templateUrl: './project-list.component.html',
  styleUrl: './project-list.component.scss',
})
export class ProjectListComponent implements OnInit {
  private readonly store = inject(ProjectStore);
  private readonly route = inject(ActivatedRoute);

  protected readonly loading = this.store.loading;
  protected readonly error = this.store.error;
  protected readonly loaded = this.store.loaded;

  protected readonly searchTerm = signal('');
  protected readonly statusFilter = signal<ProjectStatus | 'ALL'>('ALL');

  protected readonly statusOptions: { value: ProjectStatus | 'ALL'; label: string }[] = [
    { value: 'ALL', label: 'All statuses' },
    ...(Object.keys(PROJECT_STATUS_META) as ProjectStatus[]).map((status) => ({
      value: status,
      label: PROJECT_STATUS_META[status].label,
    })),
  ];

  protected readonly filteredProjects = computed<Project[]>(() => {
    const term = this.searchTerm().trim().toLowerCase();
    const status = this.statusFilter();

    return this.store
      .projects()
      .filter((project) => status === 'ALL' || project.status === status)
      .filter((project) =>
        !term
          ? true
          : [
              project.reference,
              project.consignee.name,
              project.exporter.name,
              project.destination,
            ].some((value) => value.toLowerCase().includes(term)),
      )
      .sort((a, b) => b.createdAt.localeCompare(a.createdAt));
  });

  protected readonly hasFilters = computed(
    () => this.searchTerm().trim().length > 0 || this.statusFilter() !== 'ALL',
  );

  constructor() {
    this.route.queryParamMap.pipe(takeUntilDestroyed()).subscribe((params) => {
      const q = params.get('q');
      if (q) {
        this.searchTerm.set(q);
      }
    });
  }

  ngOnInit(): void {
    this.store.loadProjects();
  }

  protected statusMeta(status: ProjectStatus) {
    return PROJECT_STATUS_META[status];
  }

  protected clearFilters(): void {
    this.searchTerm.set('');
    this.statusFilter.set('ALL');
  }

  protected readonly formatDate = formatDate;
}
