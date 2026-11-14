import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Subject, debounceTime, distinctUntilChanged } from 'rxjs';
import {
  ADDRESS_MAX_LENGTH,
  LEGAL_NAME_MAX_LENGTH,
  ORGANIZATION_ROLES,
  ORGANIZATION_ROLE_DESCRIPTIONS,
  ORGANIZATION_ROLE_LABELS,
  Organization,
  OrganizationPage,
  OrganizationRole,
  REGISTRATION_NUMBER_MAX_LENGTH,
  UpdateOrganizationRequest,
} from '../../../core/models/commercial.model';
import { isApiProblemError } from '../../../core/models/project.model';
import { ORGANIZATION_SERVICE } from '../../../core/services/organization.service';
import { NotificationService } from '../../../core/services/notification.service';
import { trimmedRequired } from '../../../core/utils/validators';
import { CountrySelectComponent } from '../../../shared/components/country-select/country-select.component';
import { IconComponent } from '../../../shared/components/icon/icon.component';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { PanelComponent } from '../../../shared/components/panel/panel.component';
import { StatusBadgeComponent } from '../../../shared/components/status-badge/status-badge.component';

type Mode = 'list' | 'create' | 'edit';

const PAGE_SIZE = 20;

@Component({
  selector: 'app-company-list',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    IconComponent,
    PageHeaderComponent,
    PanelComponent,
    StatusBadgeComponent,
    CountrySelectComponent,
  ],
  templateUrl: './company-list.component.html',
  styleUrl: './company-list.component.scss',
})
export class CompanyListComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly organizations = inject(ORGANIZATION_SERVICE);
  private readonly notifications = inject(NotificationService);

  protected readonly roles = ORGANIZATION_ROLES;
  protected readonly roleLabels = ORGANIZATION_ROLE_LABELS;
  protected readonly roleDescriptions = ORGANIZATION_ROLE_DESCRIPTIONS;
  protected readonly legalNameMaxLength = LEGAL_NAME_MAX_LENGTH;
  protected readonly registrationMaxLength = REGISTRATION_NUMBER_MAX_LENGTH;
  protected readonly addressMaxLength = ADDRESS_MAX_LENGTH;

  protected readonly page = signal<OrganizationPage>({
    items: [],
    page: 0,
    size: PAGE_SIZE,
    totalElements: 0,
    totalPages: 0,
  });
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly mode = signal<Mode>('list');
  protected readonly selected = signal<Organization | null>(null);
  protected readonly submitting = signal(false);
  protected readonly includeInactive = signal(false);
  protected readonly roleFilter = signal<OrganizationRole | ''>('');
  protected readonly searchTerm = signal('');

  protected readonly rangeStart = computed(() =>
    this.page().totalElements === 0 ? 0 : this.page().page * this.page().size + 1,
  );
  protected readonly rangeEnd = computed(() =>
    Math.min((this.page().page + 1) * this.page().size, this.page().totalElements),
  );

  protected readonly createForm = this.fb.group({
    legalName: ['', [trimmedRequired, Validators.maxLength(LEGAL_NAME_MAX_LENGTH)]],
    countryCode: [null as string | null],
    registrationNumber: ['', [Validators.maxLength(REGISTRATION_NUMBER_MAX_LENGTH)]],
    addressText: ['', [Validators.maxLength(ADDRESS_MAX_LENGTH)]],
    roles: this.rolesGroup(),
  });

  protected readonly editForm = this.fb.group({
    legalName: ['', [trimmedRequired, Validators.maxLength(LEGAL_NAME_MAX_LENGTH)]],
    countryCode: [null as string | null],
    registrationNumber: ['', [Validators.maxLength(REGISTRATION_NUMBER_MAX_LENGTH)]],
    addressText: ['', [Validators.maxLength(ADDRESS_MAX_LENGTH)]],
    active: [true],
    roles: this.rolesGroup(),
  });

  private readonly search$ = new Subject<string>();

  constructor() {
    this.search$
      .pipe(debounceTime(300), distinctUntilChanged(), takeUntilDestroyed())
      .subscribe((term) => {
        this.searchTerm.set(term);
        this.load(0);
      });
  }

  ngOnInit(): void {
    this.load(0);
  }

  protected roleLabel(role: OrganizationRole): string {
    return this.roleLabels[role];
  }

  protected onSearch(value: string): void {
    this.search$.next(value);
  }

  protected onRoleFilterChange(value: string): void {
    this.roleFilter.set(value as OrganizationRole | '');
    this.load(0);
  }

  protected onIncludeInactiveChange(checked: boolean): void {
    this.includeInactive.set(checked);
    this.load(0);
  }

  protected startCreate(): void {
    this.createForm.reset({
      legalName: '',
      countryCode: null,
      registrationNumber: '',
      addressText: '',
    });
    this.resetRoles(this.createForm.controls.roles);
    this.mode.set('create');
  }

  protected startEdit(organization: Organization): void {
    this.selected.set(organization);
    this.editForm.patchValue({
      legalName: organization.legalName,
      countryCode: organization.countryCode,
      registrationNumber: organization.registrationNumber ?? '',
      addressText: organization.addressText ?? '',
      active: organization.active,
    });
    this.resetRoles(this.editForm.controls.roles);
    for (const role of organization.roles) {
      this.editForm.controls.roles.get(role)?.setValue(true);
    }
    // An existing unconfigured organization may keep an empty role set; never
    // force a role just to edit another field.
    this.editForm.controls.roles.setValidators(
      organization.roles.length === 0 ? [] : [atLeastOneRole],
    );
    this.editForm.controls.roles.updateValueAndValidity({ emitEvent: false });
    this.mode.set('edit');
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
    this.organizations
      .create({
        legalName: (value.legalName ?? '').trim(),
        countryCode: value.countryCode,
        registrationNumber: blankToNull(value.registrationNumber),
        addressText: blankToNull(value.addressText),
        roles: this.checkedRoles(this.createForm.controls.roles),
        active: true,
      })
      .subscribe({
        next: (organization) => {
          this.submitting.set(false);
          this.notifications.success(`Company ${organization.legalName} created.`);
          this.cancel();
          this.load(0);
        },
        error: (error: unknown) => {
          this.submitting.set(false);
          this.handleError(error, this.createForm, 'The company could not be created.');
        },
      });
  }

  protected submitEdit(): void {
    const organization = this.selected();
    if (!organization || this.editForm.invalid || this.submitting()) {
      this.editForm.markAllAsTouched();
      return;
    }
    const value = this.editForm.getRawValue();
    const roles = this.checkedRoles(this.editForm.controls.roles);
    const patch: UpdateOrganizationRequest = {
      expectedVersion: organization.version,
      legalName: (value.legalName ?? '').trim(),
      countryCode: value.countryCode,
      registrationNumber: blankToNull(value.registrationNumber),
      addressText: blankToNull(value.addressText),
      active: value.active ?? true,
    };
    if (roles.length > 0 || organization.roles.length > 0) {
      patch.roles = roles;
    }
    this.submitting.set(true);
    this.organizations.update(organization.id, patch).subscribe({
      next: (updated) => {
        this.submitting.set(false);
        this.notifications.success(`Company ${updated.legalName} updated.`);
        this.cancel();
        this.load(this.page().page);
      },
      error: (error: unknown) => {
        this.submitting.set(false);
        if (isApiProblemError(error) && error.status === 409) {
          this.notifications.error(
            'This company changed since you opened it. Reloading the latest version.',
          );
          this.load(this.page().page);
          this.cancel();
          return;
        }
        this.handleError(error, this.editForm, 'The company could not be updated.');
      },
    });
  }

  protected toggleActive(organization: Organization): void {
    this.organizations
      .update(organization.id, {
        expectedVersion: organization.version,
        active: !organization.active,
      })
      .subscribe({
        next: (updated) => {
          this.notifications.success(
            updated.active
              ? `${updated.legalName} reactivated.`
              : `${updated.legalName} deactivated.`,
          );
          this.load(this.page().page);
        },
        error: (error: unknown) =>
          this.handleError(error, null, 'The company could not be updated.'),
      });
  }

  protected goToPage(page: number): void {
    if (page < 0 || page >= this.page().totalPages || page === this.page().page) {
      return;
    }
    this.load(page);
  }

  protected showError(controlName: string, form: 'create' | 'edit'): boolean {
    const control = this.formFor(form).get(controlName);
    return !!control && control.invalid && (control.dirty || control.touched);
  }

  protected showRolesError(form: 'create' | 'edit'): boolean {
    const group = this.formFor(form).get('roles');
    return !!group && group.invalid && (group.dirty || group.touched);
  }

  private load(page: number): void {
    this.loading.set(true);
    this.error.set(null);
    this.organizations
      .list({
        page,
        size: PAGE_SIZE,
        q: this.searchTerm() || undefined,
        role: this.roleFilter() || undefined,
        includeInactive: this.includeInactive(),
      })
      .subscribe({
        next: (result) => {
          this.page.set(result);
          this.loading.set(false);
        },
        error: () => {
          this.error.set('Unable to load companies. Please try again.');
          this.loading.set(false);
        },
      });
  }

  private rolesGroup(): FormGroup {
    const group = this.fb.group(
      Object.fromEntries(ORGANIZATION_ROLES.map((role) => [role, [false]])),
    );
    group.addValidators(atLeastOneRole);
    return group;
  }

  private resetRoles(group: FormGroup): void {
    for (const role of ORGANIZATION_ROLES) {
      group.get(role)?.setValue(false);
    }
  }

  private checkedRoles(group: FormGroup): OrganizationRole[] {
    return ORGANIZATION_ROLES.filter((role) => group.get(role)?.value === true);
  }

  private formFor(form: 'create' | 'edit'): FormGroup {
    return form === 'create' ? this.createForm : this.editForm;
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

function atLeastOneRole(control: AbstractControl): ValidationErrors | null {
  const group = control as FormGroup;
  const anyChecked = Object.values(group.controls).some((role) => role.value === true);
  return anyChecked ? null : { atLeastOneRole: true };
}

function blankToNull(value: string | null | undefined): string | null {
  const trimmed = (value ?? '').trim();
  return trimmed.length === 0 ? null : trimmed;
}
