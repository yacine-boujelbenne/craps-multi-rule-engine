import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Subject, debounceTime, distinctUntilChanged } from 'rxjs';
import { PROJECT_STATUS_META, ProjectStatus } from '../../../core/models/project.model';
import { AuthStore } from '../../../core/services/auth.store';
import { DirectoryStore } from '../../../core/services/directory.store';
import { ProjectStore } from '../../../core/services/project.store';
import { formatDate } from '../../../core/utils/format';
import { EmptyStateComponent } from '../../../shared/components/empty-state/empty-state.component';
import { IconComponent } from '../../../shared/components/icon/icon.component';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { StatusBadgeComponent } from '../../../shared/components/status-badge/status-badge.component';

const PAGE_SIZES = [10, 20, 50];

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
  private readonly directory = inject(DirectoryStore);
  private readonly route = inject(ActivatedRoute);
  private readonly auth = inject(AuthStore);

  protected readonly isAdmin = this.auth.isAdmin;

  private readonly searchInput = new Subject<string>();

  protected readonly page = this.store.page;
  protected readonly loading = this.store.loading;
  protected readonly error = this.store.error;
  protected readonly loaded = this.store.loaded;
  protected readonly pageSizes = PAGE_SIZES;

  protected readonly searchTerm = signal('');
  protected readonly statusFilter = signal<ProjectStatus | ''>('');

  protected readonly statusOptions: { value: ProjectStatus | ''; label: string }[] = [
    { value: '', label: 'All statuses' },
    ...(Object.keys(PROJECT_STATUS_META) as ProjectStatus[]).map((status) => ({
      value: status,
      label: PROJECT_STATUS_META[status].label,
    })),
  ];

  protected readonly hasFilters = computed(
    () => this.searchTerm().trim().length > 0 || this.statusFilter() !== '',
  );

  protected readonly isEmpty = computed(
    () => this.loaded() && !this.loading() && this.page().items.length === 0,
  );

  protected readonly rangeStart = computed(() =>
    this.page().totalElements === 0 ? 0 : this.page().page * this.page().size + 1,
  );

  protected readonly rangeEnd = computed(() =>
    Math.min((this.page().page + 1) * this.page().size, this.page().totalElements),
  );

  constructor() {
    this.searchInput
      .pipe(debounceTime(300), distinctUntilChanged(), takeUntilDestroyed())
      .subscribe(() => this.applyFilters({ page: 0 }));

    this.route.queryParamMap.pipe(takeUntilDestroyed()).subscribe((params) => {
      const q = params.get('q');
      if (q) {
        this.searchTerm.set(q);
      }
    });
  }

  ngOnInit(): void {
    this.directory.ensureLoaded();
    this.applyFilters({ page: 0 });
  }

  protected onSearch(value: string): void {
    this.searchTerm.set(value);
    this.searchInput.next(value);
  }

  protected onStatusChange(value: string): void {
    this.statusFilter.set(value as ProjectStatus | '');
    this.applyFilters({ page: 0 });
  }

  protected onPageSizeChange(size: number): void {
    this.applyFilters({ page: 0, size });
  }

  protected goToPage(page: number): void {
    if (page < 0 || page >= this.page().totalPages || page === this.page().page) {
      return;
    }
    this.applyFilters({ page });
  }

  protected clearFilters(): void {
    this.searchTerm.set('');
    this.statusFilter.set('');
    this.applyFilters({ page: 0 });
  }

  protected statusMeta(status: ProjectStatus) {
    return PROJECT_STATUS_META[status];
  }

  protected consigneeName(id: string | null | undefined): string {
    return this.directory.organizationName(id);
  }

  protected countryName(code: string | null | undefined): string {
    return this.directory.countryName(code);
  }

  private applyFilters(override: { page?: number; size?: number }): void {
    this.store.loadPage({
      page: override.page ?? 0,
      size: override.size ?? this.page().size,
      q: this.searchTerm().trim() || undefined,
      status: this.statusFilter() || undefined,
    });
  }

  protected readonly formatDate = formatDate;
}
