import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { PASSWORD_MIN_LENGTH } from '../../../core/models/account.model';
import { isApiProblemError } from '../../../core/models/project.model';
import { AuthStore } from '../../../core/services/auth.store';
import { matchesField, passwordPolicy, trimmedRequired } from '../../../core/utils/validators';
import { IconComponent } from '../../../shared/components/icon/icon.component';

@Component({
  selector: 'app-change-password',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, IconComponent],
  templateUrl: './change-password.component.html',
  styleUrl: './change-password.component.scss',
})
export class ChangePasswordComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthStore);
  private readonly router = inject(Router);

  protected readonly forced = this.auth.mustChangePassword;
  protected readonly user = this.auth.user;
  protected readonly minLength = PASSWORD_MIN_LENGTH;
  protected readonly loading = this.auth.loading;
  protected readonly error = signal<string | null>(null);

  protected readonly form = this.fb.group({
    currentPassword: ['', [trimmedRequired]],
    newPassword: ['', [trimmedRequired, passwordPolicy]],
    confirmPassword: ['', [trimmedRequired, matchesField('newPassword')]],
  });

  constructor() {
    this.form.controls.newPassword.valueChanges.pipe(takeUntilDestroyed()).subscribe(() => {
      this.form.controls.confirmPassword.updateValueAndValidity({ emitEvent: false });
    });
  }

  protected submit(): void {
    if (this.form.invalid || this.loading()) {
      this.form.markAllAsTouched();
      return;
    }
    const { currentPassword, newPassword } = this.form.getRawValue();
    this.auth
      .changePassword({ currentPassword: currentPassword ?? '', newPassword: newPassword ?? '' })
      .subscribe({
        next: () => {
          void this.router.navigate(['/login'], { queryParams: { changed: '1' } });
        },
        error: (error: unknown) => {
          this.error.set(problemDetail(error) ?? 'The password could not be changed.');
          this.mapError(error);
        },
      });
  }

  protected signOut(): void {
    this.auth.logout().subscribe(() => {
      void this.router.navigate(['/login']);
    });
  }

  private mapError(error: unknown): void {
    if (isApiProblemError(error)) {
      for (const fieldError of error.fieldErrors) {
        const control = this.form.get(fieldError.field);
        if (control) {
          control.setErrors({ server: fieldError.message });
          control.markAsTouched();
        }
      }
    }
  }
}

function problemDetail(error: unknown): string | null {
  return isApiProblemError(error) ? (error.problem.detail ?? error.problem.title ?? null) : null;
}
