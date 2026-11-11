import { ChangeDetectionStrategy, Component, computed, inject, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ProjectStore } from '../../core/services/project.store';
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

  protected readonly stats = this.store.stats;
  protected readonly recentProjects = this.store.recentProjects;
  protected readonly upcomingDepartures = this.store.upcomingDepartures;
  protected readonly loading = this.store.loading;
  protected readonly error = this.store.error;
  protected readonly loaded = this.store.loaded;

  protected readonly isEmpty = computed(() => this.loaded() && this.store.projects().length === 0);

  ngOnInit(): void {
    this.store.loadProjects();
  }

  protected statusMeta(status: Project['status']) {
    return PROJECT_STATUS_META[status];
  }

  protected readonly formatDate = formatDate;
  protected readonly relativeToToday = relativeToToday;
}
