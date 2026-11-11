import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { IconComponent, IconName } from '../icon/icon.component';

export type StatTone = 'accent' | 'neutral' | 'warning' | 'success' | 'info';

@Component({
  selector: 'app-stat-card',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [IconComponent],
  template: `
    <article class="stat" [class]="'stat--' + tone()">
      <div class="stat__icon">
        <app-icon [name]="icon()" [size]="20" />
      </div>
      <div class="stat__body">
        <span class="stat__label">{{ label() }}</span>
        <span class="stat__value">{{ value() }}</span>
        @if (hint()) {
          <span class="stat__hint">{{ hint() }}</span>
        }
      </div>
    </article>
  `,
  styles: [
    `
      .stat {
        display: flex;
        align-items: flex-start;
        gap: var(--space-3);
        padding: var(--space-4);
        background: var(--color-surface);
        border: 1px solid var(--color-border);
        border-radius: var(--radius-md);
        box-shadow: var(--shadow-xs);
        transition:
          transform var(--duration-base) var(--ease-standard),
          box-shadow var(--duration-base) var(--ease-standard);
      }

      .stat:hover {
        transform: translateY(-2px);
        box-shadow: var(--shadow-sm);
      }

      .stat__icon {
        display: grid;
        place-items: center;
        width: 40px;
        height: 40px;
        border-radius: var(--radius-sm);
        flex-shrink: 0;
      }

      .stat__body {
        display: flex;
        flex-direction: column;
        min-width: 0;
      }

      .stat__label {
        font-size: 13px;
        color: var(--color-muted);
        font-weight: 500;
      }

      .stat__value {
        font-size: 28px;
        font-weight: 600;
        line-height: 1.2;
        letter-spacing: -0.02em;
        font-variant-numeric: tabular-nums;
      }

      .stat__hint {
        font-size: 12px;
        color: var(--color-muted-soft);
        margin-top: 2px;
      }

      .stat--accent .stat__icon {
        background: var(--color-accent-soft);
        color: var(--color-accent-ink);
      }
      .stat--neutral .stat__icon {
        background: var(--color-surface-sunken);
        color: var(--color-ink-soft);
      }
      .stat--warning .stat__icon {
        background: var(--color-warning-soft);
        color: var(--color-warning);
      }
      .stat--success .stat__icon {
        background: var(--color-success-soft);
        color: var(--color-success);
      }
      .stat--info .stat__icon {
        background: var(--color-info-soft);
        color: var(--color-info);
      }
    `,
  ],
})
export class StatCardComponent {
  readonly label = input.required<string>();
  readonly value = input.required<number>();
  readonly icon = input<IconName>('folder');
  readonly tone = input<StatTone>('accent');
  readonly hint = input<string>('');
}
