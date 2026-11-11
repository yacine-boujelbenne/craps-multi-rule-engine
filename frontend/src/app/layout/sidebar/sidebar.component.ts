import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { APP_CONFIG } from '../../core/config/app-config';
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
  imports: [RouterLink, RouterLinkActive, IconComponent],
  templateUrl: './sidebar.component.html',
  styleUrl: './sidebar.component.scss',
})
export class SidebarComponent {
  readonly open = input<boolean>(false);
  readonly navigate = output<void>();

  protected readonly isMock = APP_CONFIG.useMockApi;

  protected readonly primaryNav: NavItem[] = [
    { label: 'Dashboard', icon: 'grid', link: '/dashboard' },
    { label: 'Projects', icon: 'folder', link: '/projects' },
  ];

  protected readonly secondaryNav: NavItem[] = [
    { label: 'Master data', icon: 'database', disabled: true },
    { label: 'Administration', icon: 'sliders', disabled: true },
  ];
}
