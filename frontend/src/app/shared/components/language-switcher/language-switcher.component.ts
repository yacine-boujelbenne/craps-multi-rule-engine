import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

/**
 * Compact FR/EN language toggle. Reads the active language from the
 * {@link TranslateService} signal and switches it via `use()`.
 */
@Component({
  selector: 'app-language-switcher',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslatePipe],
  template: `
    <div class="lang-switch" role="group" [attr.aria-label]="'language.label' | translate">
      @for (option of languages; track option) {
        <button
          type="button"
          class="lang-switch__btn"
          [class.lang-switch__btn--active]="current() === option"
          [attr.aria-pressed]="current() === option"
          (click)="setLanguage(option)"
        >
          {{ option }}
        </button>
      }
    </div>
  `,
  styles: [
    `
      .lang-switch {
        display: inline-flex;
        padding: 2px;
        border: 1px solid var(--color-border);
        border-radius: var(--radius-sm);
        background: var(--color-canvas);
      }

      .lang-switch__btn {
        min-width: 34px;
        padding: 4px 8px;
        border: none;
        border-radius: calc(var(--radius-sm) - 2px);
        background: transparent;
        color: var(--color-muted);
        font: inherit;
        font-size: 11px;
        font-weight: 600;
        text-transform: uppercase;
        letter-spacing: 0.04em;
        cursor: pointer;
        transition: background var(--duration-fast) var(--ease-standard);
      }

      .lang-switch__btn:hover {
        color: var(--color-ink);
      }

      .lang-switch__btn--active {
        background: var(--color-surface);
        color: var(--color-accent-ink);
        box-shadow: var(--shadow-xs, 0 1px 2px rgb(0 0 0 / 8%));
      }
    `,
  ],
})
export class LanguageSwitcherComponent {
  private readonly translate = inject(TranslateService);

  protected readonly languages = ['fr', 'en'] as const;
  protected readonly current = this.translate.currentLang;

  protected setLanguage(code: string): void {
    void this.translate.use(code);
  }
}
