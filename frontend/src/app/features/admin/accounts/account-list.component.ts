import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  ACCOUNT_ROLES,
  ACCOUNT_ROLE_DESCRIPTIONS,
  ACCOUNT_ROLE_LABELS,
  AccountRole,
  DISPLAY_NAME_MAX_LENGTH,
  PASSWORD_MIN_LENGTH,
  USERNAME_MAX_LENGTH,
  USERNAME_PATTERN,
  User,
  UserPage,
  normalizeUsername,
} from '../../../core/models/account.model';
import { isApiProblemError } from '../../../core/models/project.model';
import { ACCOUNT_SERVICE } from '../../../core/services/account.service';
import { AuthStore } from '../../../core/services/auth.store';
import { NotificationService } from '../../../core/services/notification.service';
import { matchesField, passwordPolicy, trimmedRequired } from '../../../core/utils/validators';
import { IconComponent } from '../../../shared/components/icon/icon.component';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { PanelComponent } from '../../../shared/components/panel/panel.component';
import { StatusBadgeComponent } from '../../../shared/components/status-badge/status-badge.component';

type Mode = 'list' | 'create' | 'edit' | 'reset';

const PAGE_SIZE = 20;

@Component({
  selector: 'app-account-list',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    IconComponent,
    PageHeaderComponent,
    PanelComponent,
    StatusBadgeComponent,
  ],
  templateUrl: './account-list.component.html',
  styleUrl: './account-list.component.scss',
})
export class AccountListComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly accounts = inject(ACCOUNT_SERVICE);
  private readonly notifications = inject(NotificationService);
  private readonly auth = inject(AuthStore);

  protected readonly roles = ACCOUNT_ROLES;
  protected readonly roleLabels = ACCOUNT_ROLE_LABELS;
  protected readonly roleDescriptions = ACCOUNT_ROLE_DESCRIPTIONS;
  protected readonly usernameMaxLength = USERNAME_MAX_LENGTH;
  protected readonly displayNameMaxLength = DISPLAY_NAME_MAX_LENGTH;
  protected readonly passwordMinLength = PASSWORD_MIN_LENGTH;

  protected readonly page = signal<UserPage>({
    items: [],
    page: 0,
    size: PAGE_SIZE,
    totalElements: 0,
    totalPages: 0,
  });
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly mode = signal<Mode>('list');
  protected readonly selected = signal<User | null>(null);
  protected readonly submitting = signal(false);

  protected readonly currentUserId = computed(() => this.auth.user()?.id ?? null);
  protected readonly rangeStart = computed(() =>
    this.page().totalElements === 0 ? 0 : this.page().page * this.page().size + 1,
  );
  protected readonly rangeEnd = computed(() =>
    Math.min((this.page().page + 1) * this.page().size, this.page().totalElements),
  );

  protected readonly createForm = this.fb.group({
    username: ['', [trimmedRequired, Validators.pattern(USERNAME_PATTERN)]],
    displayName: ['', [trimmedRequired, Validators.maxLength(DISPLAY_NAME_MAX_LENGTH)]],
    initialPassword: ['', [trimmedRequired, passwordPolicy]],
    confirmPassword: ['', [trimmedRequired, matchesField('initialPassword')]],
    roles: this.rolesGroup(),
  });

  protected readonly editForm = this.fb.group({
    username: ['', [trimmedRequired, Validators.pattern(USERNAME_PATTERN)]],
    displayName: ['', [trimmedRequired, Validators.maxLength(DISPLAY_NAME_MAX_LENGTH)]],
    active: [true],
    roles: this.rolesGroup(),
  });

  protected readonly resetForm = this.fb.group({
    temporaryPassword: ['', [trimmedRequired, passwordPolicy]],
    confirmPassword: ['', [trimmedRequired, matchesField('temporaryPassword')]],
  });

  constructor() {
    this.createForm.controls.initialPassword.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe(() =>
        this.createForm.controls.confirmPassword.updateValueAndValidity({ emitEvent: false }),
      );
    this.resetForm.controls.temporaryPassword.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe(() =>
        this.resetForm.controls.confirmPassword.updateValueAndValidity({ emitEvent: false }),
      );
  }

  ngOnInit(): void {
    this.load(0);
  }

  protected roleLabel(role: AccountRole): string {
    return this.roleLabels[role];
  }

  protected startCreate(): void {
    this.createForm.reset({
      username: '',
      displayName: '',
      initialPassword: '',
      confirmPassword: '',
    });
    this.resetRoles(this.createForm.controls.roles);
    this.mode.set('create');
  }

  protected startEdit(user: User): void {
    this.selected.set(user);
    this.editForm.patchValue({
      username: user.username,
      displayName: user.displayName,
      active: user.active,
    });
    this.resetRoles(this.editForm.controls.roles);
    for (const role of user.roles) {
      this.editForm.controls.roles.get(role)?.setValue(true);
    }
    this.mode.set('edit');
  }

  protected startReset(user: User): void {
    this.selected.set(user);
    this.resetForm.reset({ temporaryPassword: '', confirmPassword: '' });
    this.mode.set('reset');
  }

  protected cancel(): void {
    this.mode.set('list');
    this.selected.set(null);
  }

  protected submitCreate(): void {
    if (this.createForm.invalid || this.submitting()) {
      this.createForm.markAllAsTouched();
      return;
    }
    const value = this.createForm.getRawValue();
    this.submitting.set(true);
    this.accounts
      .create({
        username: normalizeUsername(value.username ?? ''),
        displayName: (value.displayName ?? '').trim(),
        roles: this.checkedRoles(this.createForm.controls.roles),
        initialPassword: value.initialPassword ?? '',
      })
      .subscribe({
        next: (user) => {
          this.submitting.set(false);
          this.notifications.success(`Account ${user.username} created.`);
          this.cancel();
          this.load(0);
        },
        error: (error: unknown) => {
          this.submitting.set(false);
          this.handleError(error, this.createForm, 'The account could not be created.');
        },
      });
  }

  protected submitEdit(): void {
    const user = this.selected();
    if (!user || this.editForm.invalid || this.submitting()) {
      this.editForm.markAllAsTouched();
      return;
    }
    const value = this.editForm.getRawValue();
    this.submitting.set(true);
    this.accounts
      .update(user.id, {
        expectedVersion: user.version,
        username: normalizeUsername(value.username ?? ''),
        displayName: (value.displayName ?? '').trim(),
        active: value.active ?? true,
        roles: this.checkedRoles(this.editForm.controls.roles),
      })
      .subscribe({
        next: (updated) => {
          this.submitting.set(false);
          this.notifications.success(`Account ${updated.username} updated.`);
          this.cancel();
          this.load(this.page().page);
        },
        error: (error: unknown) => {
          this.submitting.set(false);
          this.handleError(error, this.editForm, 'The account could not be updated.');
        },
      });
  }

  protected submitReset(): void {
    const user = this.selected();
    if (!user || this.resetForm.invalid || this.submitting()) {
      this.resetForm.markAllAsTouched();
      return;
    }
    const value = this.resetForm.getRawValue();
    this.submitting.set(true);
    this.accounts
      .resetPassword(user.id, {
        expectedVersion: user.version,
        temporaryPassword: value.temporaryPassword ?? '',
      })
      .subscribe({
        next: (updated) => {
          this.submitting.set(false);
          this.notifications.success(
            `Temporary password set for ${updated.username}. They must change it at next sign-in.`,
          );
          this.cancel();
          this.load(this.page().page);
        },
        error: (error: unknown) => {
          this.submitting.set(false);
          this.handleError(error, this.resetForm, 'The password could not be reset.');
        },
      });
  }

  protected toggleActive(user: User): void {
    this.accounts
      .update(user.id, { expectedVersion: user.version, active: !user.active })
      .subscribe({
        next: (updated) => {
          this.notifications.success(
            updated.active
              ? `${updated.username} reactivated.`
              : `${updated.username} deactivated.`,
          );
          this.load(this.page().page);
        },
        error: (error: unknown) =>
          this.handleError(error, null, 'The account could not be updated.'),
      });
  }

  protected goToPage(page: number): void {
    if (page < 0 || page >= this.page().totalPages || page === this.page().page) {
      return;
    }
    this.load(page);
  }

  protected showError(controlName: string, form: 'create' | 'edit' | 'reset'): boolean {
    const control = this.formFor(form).get(controlName);
    return !!control && control.invalid && (control.dirty || control.touched);
  }

  private load(page: number): void {
    this.loading.set(true);
    this.error.set(null);
    this.accounts.list(page, PAGE_SIZE).subscribe({
      next: (result) => {
        this.page.set(result);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Unable to load accounts. Please try again.');
        this.loading.set(false);
      },
    });
  }

  private rolesGroup() {
    return this.fb.group({
      ADMIN: [false],
      OPERATOR: [false],
      REVIEWER: [false],
      VIEWER: [false],
    });
  }

  private resetRoles(group: ReturnType<AccountListComponent['rolesGroup']>): void {
    for (const role of ACCOUNT_ROLES) {
      group.get(role)?.setValue(false);
    }
  }

  private checkedRoles(group: ReturnType<AccountListComponent['rolesGroup']>): AccountRole[] {
    return ACCOUNT_ROLES.filter((role) => group.get(role)?.value === true);
  }

  private formFor(form: 'create' | 'edit' | 'reset'): FormGroup {
    return form === 'create' ? this.createForm : form === 'edit' ? this.editForm : this.resetForm;
  }

  private handleError(error: unknown, form: FormGroup | null, fallback: string): void {
    if (isApiProblemError(error)) {
      if (form) {
        for (const fieldError of error.fieldErrors) {
          const control = form.get(fieldError.field);
          if (control) {
            control.setErrors({ server: fieldError.message });
            control.markAsTouched();
          }
        }
      }
      this.notifications.error(error.problem.detail || error.problem.title || fallback);
      return;
    }
    this.notifications.error(fallback);
  }
}
