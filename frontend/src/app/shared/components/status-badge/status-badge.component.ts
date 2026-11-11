import { ChangeDetectionStrategy, Component, input } from '@angular/core';

export type BadgeTone = 'neutral' | 'info' | 'progress' | 'warning' | 'success' | 'danger';

@Component({
  selector: 'app-status-badge',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <span class="badge" [class]="'badge--' + tone()">
      <span class="badge__dot" aria-hidden="true"></span>
      {{ label() }}
    </span>
  `,
  styles: [
    `
      .badge {
        display: inline-flex;
        align-items: center;
        gap: var(--space-2);
        padding: 3px 10px 3px 8px;
        border-radius: var(--radius-pill);
        font-size: 12px;
        font-weight: 500;
        line-height: 1.4;
        white-space: nowrap;
        border: 1px solid transparent;
      }

      .badge__dot {
        width: 6px;
        height: 6px;
        border-radius: 50%;
        background: currentColor;
        opacity: 0.85;
      }

      .badge--neutral {
        color: var(--color-ink-soft);
        background: var(--color-surface-sunken);
        border-color: var(--color-border);
      }
      .badge--info {
        color: var(--color-info);
        background: var(--color-info-soft);
      }
      .badge--progress {
        color: var(--color-accent-ink);
        background: var(--color-accent-soft);
      }
      .badge--warning {
        color: var(--color-warning);
        background: var(--color-warning-soft);
      }
      .badge--success {
        color: var(--color-success);
        background: var(--color-success-soft);
      }
      .badge--danger {
        color: var(--color-danger);
        background: var(--color-danger-soft);
      }
    `,
  ],
})
export class StatusBadgeComponent {
  readonly label = input.required<string>();
  readonly tone = input<BadgeTone>('neutral');
}
