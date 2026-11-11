import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';

export interface Breadcrumb {
  label: string;
  link?: string;
}

@Component({
  selector: 'app-page-header',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink],
  template: `
    @if (breadcrumbs().length) {
      <nav class="crumbs" aria-label="Breadcrumb">
        @for (crumb of breadcrumbs(); track crumb.label; let last = $last) {
          @if (crumb.link && !last) {
            <a [routerLink]="crumb.link">{{ crumb.label }}</a>
          } @else {
            <span [attr.aria-current]="last ? 'page' : null">{{ crumb.label }}</span>
          }
          @if (!last) {
            <span class="crumbs__sep" aria-hidden="true">/</span>
          }
        }
      </nav>
    }

    <header class="page-header">
      <div class="page-header__text">
        <h1>{{ title() }}</h1>
        @if (subtitle()) {
          <p class="page-header__subtitle">{{ subtitle() }}</p>
        }
      </div>
      <div class="page-header__actions">
        <ng-content />
      </div>
    </header>
  `,
  styles: [
    `
      .crumbs {
        display: flex;
        align-items: center;
        gap: var(--space-2);
        font-size: 13px;
        color: var(--color-muted);
        margin-bottom: var(--space-3);
      }

      .crumbs a {
        color: var(--color-muted);
      }

      .crumbs a:hover {
        color: var(--color-accent-strong);
      }

      .crumbs__sep {
        color: var(--color-border-strong);
      }

      .page-header {
        display: flex;
        align-items: flex-start;
        justify-content: space-between;
        gap: var(--space-4);
        flex-wrap: wrap;
        margin-bottom: var(--space-5);
      }

      h1 {
        font-size: 24px;
        line-height: 1.25;
      }

      .page-header__subtitle {
        margin: var(--space-1) 0 0;
        color: var(--color-muted);
        font-size: 14px;
      }

      .page-header__actions {
        display: flex;
        align-items: center;
        gap: var(--space-2);
        flex-wrap: wrap;
      }

      @media (max-width: 599px) {
        h1 {
          font-size: 20px;
        }
      }
    `,
  ],
})
export class PageHeaderComponent {
  readonly title = input.required<string>();
  readonly subtitle = input<string>('');
  readonly breadcrumbs = input<Breadcrumb[]>([]);
}
