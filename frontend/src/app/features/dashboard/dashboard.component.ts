import { ChangeDetectionStrategy, Component, computed, inject, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ProjectStore } from '../../core/services/project.store';
import { DirectoryStore } from '../../core/services/directory.store';
import { AuthStore } from '../../core/services/auth.store';
import { PROJECT_STATUS_META, Project } from '../../core/models/project.model';
import { formatDate, relativeToToday } from '../../core/utils/format';
import { IconComponent } from '../../shared/components/icon/icon.component';
import { PageHeaderComponent } from '../../shared/components/page-header/page-header.component';
import { PanelComponent } from '../../shared/components/panel/panel.component';
import { StatCardComponent } from '../../shared/components/stat-card/stat-card.component';
import { StatusBadgeComponent } from '../../shared/components/status-badge/status-badge.component';

@Component({
  selector: 'app-dashboard',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterLink,
    IconComponent,
    PageHeaderComponent,
    PanelComponent,
    StatCardComponent,
    StatusBadgeComponent,
  ],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss',
})
export class DashboardComponent implements OnInit {
  private readonly store = inject(ProjectStore);
  private readonly directory = inject(DirectoryStore);
  private readonly auth = inject(AuthStore);

  protected readonly isAdmin = this.auth.isAdmin;

  protected readonly stats = this.store.stats;
  protected readonly recentProjects = this.store.recentProjects;
  protected readonly upcomingDepartures = this.store.upcomingDepartures;
  protected readonly loading = this.store.loading;
  protected readonly error = this.store.error;
  protected readonly loaded = this.store.loaded;

  protected readonly isEmpty = computed(() => this.loaded() && this.stats().totalProjects === 0);

  ngOnInit(): void {
    this.directory.ensureLoaded();
    this.store.loadDashboard();
  }

  protected statusMeta(status: Project['status']) {
    return PROJECT_STATUS_META[status];
  }

  protected consigneeName(id: string | null | undefined): string {
    return this.directory.organizationName(id);
  }

  protected countryName(code: string | null | undefined): string {
    return this.directory.countryName(code);
  }

  protected readonly formatDate = formatDate;
  protected readonly relativeToToday = relativeToToday;
}
