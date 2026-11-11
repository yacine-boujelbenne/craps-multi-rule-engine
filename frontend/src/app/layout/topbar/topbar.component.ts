import { ChangeDetectionStrategy, Component, inject, output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { IconComponent } from '../../shared/components/icon/icon.component';

@Component({
  selector: 'app-topbar',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, IconComponent],
  template: `
    <header class="topbar">
      <button
        type="button"
        class="topbar__icon-btn topbar__menu"
        aria-label="Toggle navigation"
        (click)="menuToggle.emit()"
      >
        <app-icon name="menu" [size]="20" />
      </button>

      <form class="topbar__search" role="search" (ngSubmit)="onSearch()">
        <app-icon name="search" [size]="18" />
        <input
          type="search"
          name="q"
          [(ngModel)]="query"
          placeholder="Search projects, references, customers…"
          aria-label="Search projects"
        />
      </form>

      <div class="topbar__right">
        <button type="button" class="topbar__icon-btn" aria-label="Notifications">
          <app-icon name="bell" [size]="20" />
          <span class="topbar__badge" aria-hidden="true"></span>
        </button>

        <div class="topbar__account">
          <span class="topbar__avatar" aria-hidden="true">OP</span>
          <span class="topbar__account-text">
            <strong>Operator</strong>
            <small>Documentation</small>
          </span>
        </div>
      </div>
    </header>
  `,
  styles: [
    `
      .topbar {
        display: flex;
        align-items: center;
        gap: var(--space-4);
        height: var(--topbar-height);
        padding: 0 var(--space-5);
        background: var(--color-surface);
        border-bottom: 1px solid var(--color-border);
        position: sticky;
        top: 0;
        z-index: 20;
      }

      .topbar__menu {
        display: none;
      }

      .topbar__search {
        display: flex;
        align-items: center;
        gap: var(--space-2);
        flex: 1;
        max-width: 420px;
        padding: 0 var(--space-3);
        height: 38px;
        border: 1px solid var(--color-border);
        border-radius: var(--radius-sm);
        background: var(--color-canvas);
        color: var(--color-muted);
        transition: border-color var(--duration-fast) var(--ease-standard);
      }

      .topbar__search:focus-within {
        border-color: var(--color-accent-strong);
        background: var(--color-surface);
      }

      .topbar__search input {
        border: none;
        outline: none;
        background: transparent;
        flex: 1;
        font-size: 14px;
        font-family: inherit;
        color: var(--color-ink);
        min-width: 0;
      }

      .topbar__search input::placeholder {
        color: var(--color-muted-soft);
      }

      .topbar__right {
        display: flex;
        align-items: center;
        gap: var(--space-3);
        margin-left: auto;
      }

      .topbar__icon-btn {
        position: relative;
        display: grid;
        place-items: center;
        width: 38px;
        height: 38px;
        border: 1px solid transparent;
        border-radius: var(--radius-sm);
        background: transparent;
        color: var(--color-ink-soft);
        cursor: pointer;
        transition: background var(--duration-fast) var(--ease-standard);
      }

      .topbar__icon-btn:hover {
        background: var(--color-surface-sunken);
      }

      .topbar__badge {
        position: absolute;
        top: 9px;
        right: 10px;
        width: 7px;
        height: 7px;
        border-radius: 50%;
        background: var(--color-accent-strong);
        border: 2px solid var(--color-surface);
      }

      .topbar__account {
        display: flex;
        align-items: center;
        gap: var(--space-2);
        padding-left: var(--space-3);
        border-left: 1px solid var(--color-border);
      }

      .topbar__avatar {
        display: grid;
        place-items: center;
        width: 34px;
        height: 34px;
        border-radius: 50%;
        background: var(--color-accent-soft);
        color: var(--color-accent-ink);
        font-size: 12px;
        font-weight: 600;
      }

      .topbar__account-text {
        display: flex;
        flex-direction: column;
        line-height: 1.2;
      }

      .topbar__account-text strong {
        font-size: 13px;
        font-weight: 600;
      }

      .topbar__account-text small {
        font-size: 11px;
        color: var(--color-muted);
      }

      @media (max-width: 959px) {
        .topbar {
          padding: 0 var(--space-4);
        }

        .topbar__menu {
          display: grid;
        }

        .topbar__account-text {
          display: none;
        }
      }

      @media (max-width: 599px) {
        .topbar__search {
          display: none;
        }
      }
    `,
  ],
})
export class TopbarComponent {
  readonly menuToggle = output<void>();

  protected query = '';

  private readonly router = inject(Router);

  protected onSearch(): void {
    const value = this.query.trim();
    this.router.navigate(['/projects'], value ? { queryParams: { q: value } } : {});
  }
}
