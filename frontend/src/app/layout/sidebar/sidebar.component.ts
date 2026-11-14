import { ChangeDetectionStrategy, Component, computed, inject, input, output } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { APP_CONFIG } from '../../core/config/app-config';
import { AuthStore } from '../../core/services/auth.store';
import { IconComponent, IconName } from '../../shared/components/icon/icon.component';

interface NavItem {
  label: string;
  icon: IconName;
  link?: string;
  disabled?: boolean;
}

@Component({
  selector: 'app-sidebar',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, RouterLinkActive, IconComponent, TranslatePipe],
  templateUrl: './sidebar.component.html',
  styleUrl: './sidebar.component.scss',
})
export class SidebarComponent {
  readonly open = input<boolean>(false);
  readonly navigate = output<void>();

  private readonly auth = inject(AuthStore);

  protected readonly isMock = APP_CONFIG.useMockApi;

  protected readonly primaryNav: NavItem[] = [
    { label: 'nav.dashboard', icon: 'grid', link: '/dashboard' },
    { label: 'nav.projects', icon: 'folder', link: '/projects' },
  ];

  protected readonly secondaryNav = computed<NavItem[]>(() => {
    const items: NavItem[] = [];
    if (this.auth.isAdmin()) {
      items.push({ label: 'nav.companies', icon: 'building', link: '/partners' });
      items.push({ label: 'nav.products', icon: 'database', link: '/catalog' });
      items.push({ label: 'nav.accounts', icon: 'users', link: '/admin/accounts' });
    }
    return items;
  });
}
