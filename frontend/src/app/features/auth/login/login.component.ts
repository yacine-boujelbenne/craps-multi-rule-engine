import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { normalizeUsername } from '../../../core/models/account.model';
import { AuthStore } from '../../../core/services/auth.store';
import { trimmedRequired } from '../../../core/utils/validators';
import { IconComponent } from '../../../shared/components/icon/icon.component';

@Component({
  selector: 'app-login',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, IconComponent],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss',
})
export class LoginComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthStore);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  protected readonly isMock = false;
  protected readonly loading = this.auth.loading;
  protected readonly error = this.auth.error;

  protected readonly form = this.fb.group({
    username: ['', [trimmedRequired]],
    password: ['', [trimmedRequired]],
  });

  protected submit(): void {
    if (this.form.invalid || this.loading()) {
      this.form.markAllAsTouched();
      return;
    }
    const { username, password } = this.form.getRawValue();
    this.auth
      .login({ username: normalizeUsername(username ?? ''), password: password ?? '' })
      .subscribe({
        next: () => {
          const target = this.auth.mustChangePassword() ? '/change-password' : this.returnUrl();
          void this.router.navigateByUrl(target);
        },
        error: () => {
          // The store exposes the message via `error()`.
        },
      });
  }

  private returnUrl(): string {
    const requested = this.route.snapshot.queryParamMap.get('returnUrl');
    return requested && requested.startsWith('/') ? requested : '/dashboard';
  }
}
