import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { IconComponent } from '../../shared/components/icon/icon.component';

@Component({
  selector: 'app-not-found',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, IconComponent],
  template: `
    <div class="nf">
      <div class="nf__code">404</div>
      <h1>Page not found</h1>
      <p>The page you are looking for does not exist or has been moved.</p>
      <a class="btn btn--primary" routerLink="/dashboard">
        <app-icon name="grid" [size]="18" />
        Back to dashboard
      </a>
    </div>
  `,
  styles: [
    `
      .nf {
        min-height: 100vh;
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        text-align: center;
        gap: var(--space-3);
        padding: var(--space-6);
        background: var(--color-canvas);
      }

      .nf__code {
        font-size: 64px;
        font-weight: 700;
        letter-spacing: -0.04em;
        color: var(--color-accent);
        line-height: 1;
      }

      h1 {
        font-size: 22px;
      }

      p {
        color: var(--color-muted);
        margin: 0 0 var(--space-2);
      }
    `,
  ],
})
export class NotFoundComponent {}
