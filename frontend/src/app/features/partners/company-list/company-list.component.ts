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
import { Subject, debounceTime, distinctUntilChanged, of, switchMap } from 'rxjs';
import {
  ADDRESS_MAX_LENGTH,
  EMAIL_MAX_LENGTH,
  LEGAL_NAME_MAX_LENGTH,
  ORGANIZATION_ROLES,
  ORGANIZATION_ROLE_DESCRIPTIONS,
  ORGANIZATION_ROLE_LABELS,
  Organization,
  OrganizationPage,
  OrganizationRole,
  PHONE_MAX_LENGTH,
  REGISTRATION_NUMBER_MAX_LENGTH,
  TAX_IDENTIFIER_MAX_LENGTH,
  TRADE_NAME_MAX_LENGTH,
  UpdateOrganizationRequest,
} from '../../../core/models/commercial.model';
import {
  Address,
  AddressKind,
  ADDRESS_KINDS,
  ADDRESS_KIND_LABELS,
  ADDRESS_LINE_MAX_LENGTH,
  CITY_NAME_MAX_LENGTH,
  City,
  normalizePostalCode,
  nullIfBlank,
  POSTAL_CODE_MAX_LENGTH,
} from '../../../core/models/master-data.model';
import { countryDialCode } from '../../../core/data/country-dial-codes';
import { isApiProblemError } from '../../../core/models/project.model';
import { AuthStore } from '../../../core/services/auth.store';
import { COMPANY_ADDRESS_SERVICE } from '../../../core/services/company-address.service';
import { GEOGRAPHY_SERVICE } from '../../../core/services/geography.service';
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
  private readonly addresses = inject(COMPANY_ADDRESS_SERVICE);
  private readonly geography = inject(GEOGRAPHY_SERVICE);
  private readonly auth = inject(AuthStore);
  private readonly notifications = inject(NotificationService);

  protected readonly canEdit = computed(() => this.auth.isAdmin() || this.auth.hasRole('OPERATOR'));

  protected readonly roles = ORGANIZATION_ROLES;
  protected readonly roleLabels = ORGANIZATION_ROLE_LABELS;
  protected readonly roleDescriptions = ORGANIZATION_ROLE_DESCRIPTIONS;
  protected readonly legalNameMaxLength = LEGAL_NAME_MAX_LENGTH;
  protected readonly registrationMaxLength = REGISTRATION_NUMBER_MAX_LENGTH;
  protected readonly addressMaxLength = ADDRESS_MAX_LENGTH;
  protected readonly tradeNameMaxLength = TRADE_NAME_MAX_LENGTH;
  protected readonly taxIdentifierMaxLength = TAX_IDENTIFIER_MAX_LENGTH;
  protected readonly emailMaxLength = EMAIL_MAX_LENGTH;
  protected readonly phoneMaxLength = PHONE_MAX_LENGTH;
  protected readonly addressKinds = ADDRESS_KINDS;
  protected readonly addressKindLabels = ADDRESS_KIND_LABELS;
  protected readonly addressLineMaxLength = ADDRESS_LINE_MAX_LENGTH;
  protected readonly postalCodeMaxLength = POSTAL_CODE_MAX_LENGTH;
  protected readonly manualCityMaxLength = CITY_NAME_MAX_LENGTH;

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

  protected readonly companyAddresses = signal<Address[]>([]);
  protected readonly addressesLoading = signal(false);
  protected readonly addressError = signal<string | null>(null);
  protected readonly addressMode = signal<'list' | 'form'>('list');
  protected readonly editingAddress = signal<Address | null>(null);
  protected readonly addressSubmitting = signal(false);
  protected readonly cityResults = signal<City[]>([]);
  private readonly cityNames = signal<ReadonlyMap<string, string>>(new Map());
  private readonly loadedCityCountries = new Set<string>();

  protected readonly rangeStart = computed(() =>
    this.page().totalElements === 0 ? 0 : this.page().page * this.page().size + 1,
  );
  protected readonly rangeEnd = computed(() =>
    Math.min((this.page().page + 1) * this.page().size, this.page().totalElements),
  );

  protected readonly createForm = this.fb.group({
    legalName: ['', [trimmedRequired, Validators.maxLength(LEGAL_NAME_MAX_LENGTH)]],
    tradeName: ['', [Validators.maxLength(TRADE_NAME_MAX_LENGTH)]],
    registrationNumber: ['', [Validators.maxLength(REGISTRATION_NUMBER_MAX_LENGTH)]],
    taxIdentifier: ['', [Validators.maxLength(TAX_IDENTIFIER_MAX_LENGTH)]],
    email: ['', [Validators.email, Validators.maxLength(EMAIL_MAX_LENGTH)]],
    phone: ['', [Validators.maxLength(PHONE_MAX_LENGTH)]],
    countryCode: [null as string | null],
    addressText: ['', [Validators.maxLength(ADDRESS_MAX_LENGTH)]],
    roles: this.rolesGroup(),
  });

  protected readonly editForm = this.fb.group({
    legalName: ['', [trimmedRequired, Validators.maxLength(LEGAL_NAME_MAX_LENGTH)]],
    tradeName: ['', [Validators.maxLength(TRADE_NAME_MAX_LENGTH)]],
    registrationNumber: ['', [Validators.maxLength(REGISTRATION_NUMBER_MAX_LENGTH)]],
    taxIdentifier: ['', [Validators.maxLength(TAX_IDENTIFIER_MAX_LENGTH)]],
    email: ['', [Validators.email, Validators.maxLength(EMAIL_MAX_LENGTH)]],
    phone: ['', [Validators.maxLength(PHONE_MAX_LENGTH)]],
    countryCode: [null as string | null],
    addressText: ['', [Validators.maxLength(ADDRESS_MAX_LENGTH)]],
    active: [true],
    roles: this.rolesGroup(),
  });

  protected readonly addressForm = this.fb.group({
    kind: ['BUSINESS' as AddressKind],
    countryCode: [null as string | null],
    cityQuery: [''],
    cityId: [null as string | null],
    manualCity: ['', [Validators.maxLength(CITY_NAME_MAX_LENGTH)]],
    addressLine1: ['', [trimmedRequired, Validators.maxLength(ADDRESS_LINE_MAX_LENGTH)]],
    addressLine2: ['', [Validators.maxLength(ADDRESS_LINE_MAX_LENGTH)]],
    postalCode: ['', [Validators.maxLength(POSTAL_CODE_MAX_LENGTH)]],
  });

  private readonly search$ = new Subject<string>();
  private readonly citySearch$ = new Subject<string>();

  constructor() {
    this.search$
      .pipe(debounceTime(300), distinctUntilChanged(), takeUntilDestroyed())
      .subscribe((term) => {
        this.searchTerm.set(term);
        this.load(0);
      });
    this.addressForm.controls.countryCode.valueChanges.pipe(takeUntilDestroyed()).subscribe(() => {
      this.addressForm.controls.cityId.setValue(null, { emitEvent: false });
      this.cityResults.set([]);
    });
    this.citySearch$
      .pipe(
        debounceTime(250),
        distinctUntilChanged(),
        switchMap((term) =>
          term.trim() && this.addressForm.controls.countryCode.value
            ? this.geography.searchCities(this.addressForm.controls.countryCode.value ?? '', {
                q: term,
                size: 10,
              })
            : of(null),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((result) => this.cityResults.set(result ? result.items : []));
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
      tradeName: '',
      registrationNumber: '',
      taxIdentifier: '',
      email: '',
      phone: '',
      countryCode: null,
      addressText: '',
    });
    this.resetRoles(this.createForm.controls.roles);
    this.mode.set('create');
  }

  protected startEdit(organization: Organization): void {
    this.selected.set(organization);
    this.editForm.patchValue({
      legalName: organization.legalName,
      tradeName: organization.tradeName ?? '',
      registrationNumber: organization.registrationNumber ?? '',
      taxIdentifier: organization.taxIdentifier ?? '',
      email: organization.email ?? '',
      phone: organization.phone ?? '',
      countryCode: organization.countryCode,
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
    this.resetAddressEditor();
    this.loadAddresses(organization.id);
    this.mode.set('edit');
  }

  protected cancel(): void {
    this.mode.set('list');
    this.selected.set(null);
    this.resetAddressEditor();
    this.companyAddresses.set([]);
    this.addressError.set(null);
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
        tradeName: blankToNull(value.tradeName),
        countryCode: value.countryCode,
        registrationNumber: blankToNull(value.registrationNumber),
        taxIdentifier: blankToNull(value.taxIdentifier),
        email: blankToNull(value.email),
        phone: blankToNull(value.phone),
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
      tradeName: blankToNull(value.tradeName),
      countryCode: value.countryCode,
      registrationNumber: blankToNull(value.registrationNumber),
      taxIdentifier: blankToNull(value.taxIdentifier),
      email: blankToNull(value.email),
      phone: blankToNull(value.phone),
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

  protected startAddAddress(): void {
    this.resetAddressEditor();
    this.addressForm.patchValue({ countryCode: this.editForm.controls.countryCode.value });
    this.addressMode.set('form');
  }

  protected startEditAddress(address: Address): void {
    this.editingAddress.set(address);
    this.addressForm.reset({
      kind: address.kind,
      countryCode: address.countryCode,
      cityQuery: '',
      cityId: address.cityId,
      manualCity: address.manualCity ?? '',
      addressLine1: address.addressLine1,
      addressLine2: address.addressLine2 ?? '',
      postalCode: address.postalCode ?? '',
    });
    this.cityResults.set([]);
    this.addressMode.set('form');
    this.resolveCityNames([address]);
  }

  protected cancelAddress(): void {
    this.resetAddressEditor();
  }

  protected onCityQuery(value: string): void {
    this.citySearch$.next(value);
  }

  protected selectCity(city: City): void {
    this.addressForm.patchValue({
      cityId: city.id,
      manualCity: '',
      cityQuery: city.name,
    });
    const names = new Map(this.cityNames());
    names.set(city.id, city.name);
    this.cityNames.set(names);
    this.cityResults.set([]);
  }

  protected onManualCityInput(value: string): void {
    if (this.addressForm.controls.cityId.value && value.trim().length > 0) {
      this.addressForm.controls.cityId.setValue(null);
    }
  }

  protected submitAddress(): void {
    const organization = this.selected();
    if (!organization || this.addressForm.invalid || this.addressSubmitting()) {
      this.addressForm.markAllAsTouched();
      return;
    }
    const value = this.addressForm.getRawValue();
    const manualCity = nullIfBlank(value.manualCity);
    const cityId = manualCity ? null : value.cityId;
    if (!manualCity && !cityId) {
      this.notifications.error('Select a city or enter a manual city name.');
      return;
    }
    if (!value.countryCode) {
      this.notifications.error('Choose the address country.');
      return;
    }
    const editing = this.editingAddress();
    this.addressSubmitting.set(true);
    const request = editing
      ? this.addresses.update(organization.id, editing.id, {
          expectedVersion: editing.version,
          kind: value.kind ?? editing.kind,
          countryCode: value.countryCode,
          cityId,
          manualCity,
          addressLine1: (value.addressLine1 ?? '').trim(),
          addressLine2: nullIfBlank(value.addressLine2),
          postalCode: normalizePostalCode(value.postalCode),
        })
      : this.addresses.create(organization.id, {
          kind: value.kind ?? 'BUSINESS',
          countryCode: value.countryCode,
          cityId,
          manualCity,
          addressLine1: (value.addressLine1 ?? '').trim(),
          addressLine2: nullIfBlank(value.addressLine2),
          postalCode: normalizePostalCode(value.postalCode),
          expectedOrganizationVersion: organization.version,
        });
    request.subscribe({
      next: () => {
        this.addressSubmitting.set(false);
        this.notifications.success(editing ? 'Address updated.' : 'Address added.');
        this.resetAddressEditor();
        this.loadAddresses(organization.id);
        this.load(this.page().page);
      },
      error: (error: unknown) => {
        this.addressSubmitting.set(false);
        this.addressError.set(null);
        this.handleError(error, this.addressForm, 'The address could not be saved.');
      },
    });
  }

  protected addressCity(address: Address): string {
    if (address.manualCity) {
      return address.manualCity;
    }
    return this.cityLabel(address.cityId);
  }

  private loadAddresses(organizationId: string): void {
    this.addressesLoading.set(true);
    this.addressError.set(null);
    this.addresses.list(organizationId).subscribe({
      next: (items) => {
        this.companyAddresses.set(items);
        this.addressesLoading.set(false);
        this.resolveCityNames(items);
      },
      error: () => {
        this.addressError.set('Unable to load addresses.');
        this.addressesLoading.set(false);
      },
    });
  }

  private resetAddressEditor(): void {
    this.addressMode.set('list');
    this.editingAddress.set(null);
    this.cityResults.set([]);
    this.addressForm.reset({
      kind: 'BUSINESS',
      countryCode: null,
      cityQuery: '',
      cityId: null,
      manualCity: '',
      addressLine1: '',
      addressLine2: '',
      postalCode: '',
    });
  }

  private cityLabel(cityId: string | null): string {
    if (!cityId) {
      return '';
    }
    return (
      this.cityNames().get(cityId) ??
      this.cityResults().find((city) => city.id === cityId)?.name ??
      'Selected city'
    );
  }

  protected phonePlaceholder(form: 'create' | 'edit'): string {
    const dial = countryDialCode(this.formFor(form).controls['countryCode'].value as string | null);
    return dial ? `${dial} phone number` : 'Phone number';
  }

  private resolveCityNames(addresses: Address[]): void {
    const countries = new Set(
      addresses
        .filter((address) => address.cityId && !address.manualCity)
        .map((address) => address.countryCode),
    );
    for (const countryCode of countries) {
      if (this.loadedCityCountries.has(countryCode)) {
        continue;
      }
      this.loadedCityCountries.add(countryCode);
      this.fetchCityNames(countryCode, 0);
    }
  }

  private fetchCityNames(countryCode: string, page: number): void {
    this.geography.searchCities(countryCode, { page, size: 100 }).subscribe({
      next: (result) => {
        const names = new Map(this.cityNames());
        for (const city of result.items) {
          names.set(city.id, city.name);
        }
        this.cityNames.set(names);
        this.applyResolvedCityQuery();
        if (page + 1 < result.totalPages) {
          this.fetchCityNames(countryCode, page + 1);
        }
      },
      error: () => this.loadedCityCountries.delete(countryCode),
    });
  }

  private applyResolvedCityQuery(): void {
    const editing = this.editingAddress();
    if (
      !editing?.cityId ||
      this.addressMode() !== 'form' ||
      this.addressForm.controls.cityQuery.value
    ) {
      return;
    }
    const name = this.cityNames().get(editing.cityId);
    if (name) {
      this.addressForm.controls.cityQuery.setValue(name, { emitEvent: false });
    }
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
