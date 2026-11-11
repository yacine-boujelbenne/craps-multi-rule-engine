import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { IconComponent, IconName } from '../icon/icon.component';

@Component({
  selector: 'app-empty-state',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [IconComponent],
  template: `
    <div class="empty">
      <div class="empty__icon">
        <app-icon [name]="icon()" [size]="26" />
      </div>
      <h3>{{ title() }}</h3>
      @if (message()) {
        <p>{{ message() }}</p>
      }
      <div class="empty__actions">
        <ng-content />
      </div>
    </div>
  `,
  styles: [
    `
      .empty {
        display: flex;
        flex-direction: column;
        align-items: center;
        text-align: center;
        gap: var(--space-2);
        padding: var(--space-7) var(--space-5);
        color: var(--color-muted);
      }

      .empty__icon {
        display: grid;
        place-items: center;
        width: 56px;
        height: 56px;
        border-radius: 50%;
        background: var(--color-surface-sunken);
        color: var(--color-muted);
        margin-bottom: var(--space-2);
      }

      h3 {
        font-size: 16px;
        color: var(--color-ink);
      }

      p {
        margin: 0;
        max-width: 420px;
        font-size: 14px;
      }

      .empty__actions:not(:empty) {
        margin-top: var(--space-3);
      }
    `,
  ],
})
export class EmptyStateComponent {
  readonly title = input.required<string>();
  readonly message = input<string>('');
  readonly icon = input<IconName>('inbox');
}
