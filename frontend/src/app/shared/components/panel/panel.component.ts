import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/** Rounded content panel with an optional titled header and action slot. */
@Component({
  selector: 'app-panel',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="panel">
      @if (title() || subtitle()) {
        <header class="panel__header">
          <div class="panel__heading">
            @if (title()) {
              <h2>{{ title() }}</h2>
            }
            @if (subtitle()) {
              <p>{{ subtitle() }}</p>
            }
          </div>
          <div class="panel__actions">
            <ng-content select="[panel-actions]" />
          </div>
        </header>
      }
      <div class="panel__body" [class.panel__body--flush]="flush()">
        <ng-content />
      </div>
    </section>
  `,
  styles: [
    `
      .panel {
        background: var(--color-surface);
        border: 1px solid var(--color-border);
        border-radius: var(--radius-md);
        box-shadow: var(--shadow-xs);
        overflow: hidden;
      }

      .panel__header {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: var(--space-4);
        padding: var(--space-4) var(--space-5);
        border-bottom: 1px solid var(--color-border);
        flex-wrap: wrap;
      }

      .panel__heading h2 {
        font-size: 15px;
        font-weight: 600;
      }

      .panel__heading p {
        margin: 2px 0 0;
        font-size: 13px;
        color: var(--color-muted);
      }

      .panel__actions {
        display: flex;
        align-items: center;
        gap: var(--space-2);
      }

      .panel__body {
        padding: var(--space-5);
      }

      .panel__body--flush {
        padding: 0;
      }
    `,
  ],
})
export class PanelComponent {
  readonly title = input<string>('');
  readonly subtitle = input<string>('');
  readonly flush = input<boolean>(false);
}
