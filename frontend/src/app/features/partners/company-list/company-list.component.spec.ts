import { TestBed } from '@angular/core/testing';
import { FormGroup } from '@angular/forms';
import { of } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { Organization, OrganizationPage } from '../../../core/models/commercial.model';
import { AuthStore } from '../../../core/services/auth.store';
import { COMPANY_ADDRESS_SERVICE } from '../../../core/services/company-address.service';
import { GEOGRAPHY_SERVICE } from '../../../core/services/geography.service';
import { NotificationService } from '../../../core/services/notification.service';
import { ORGANIZATION_SERVICE } from '../../../core/services/organization.service';
import { CompanyListComponent } from './company-list.component';

const EMPTY_PAGE: OrganizationPage = {
  items: [],
  page: 0,
  size: 20,
  totalElements: 0,
  totalPages: 0,
};

const CREATED: Organization = {
  id: '99999999-9999-4999-8999-999999999999',
  version: 0,
  legalName: 'Atlantic Fisheries',
  countryCode: 'MA',
  addressText: null,
  registrationNumber: null,
  roles: ['SUPPLIER'],
  active: true,
  createdAt: '2026-01-01T00:00:00.000Z',
  updatedAt: '2026-01-01T00:00:00.000Z',
};

const UNCONFIGURED: Organization = {
  ...CREATED,
  id: '88888888-8888-4888-8888-888888888888',
  legalName: 'Legacy Importer',
  roles: [],
};

type TestableCompanyList = {
  startCreate(): void;
  submitCreate(): void;
  startEdit(organization: Organization): void;
  submitEdit(): void;
  createForm: FormGroup;
  editForm: FormGroup;
};

describe('CompanyListComponent', () => {
  let organizations: {
    list: ReturnType<typeof vi.fn>;
    get: ReturnType<typeof vi.fn>;
    create: ReturnType<typeof vi.fn>;
    update: ReturnType<typeof vi.fn>;
  };
  let notifications: { success: ReturnType<typeof vi.fn>; error: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    organizations = {
      list: vi.fn(() => of(EMPTY_PAGE)),
      get: vi.fn(),
      create: vi.fn(() => of(CREATED)),
      update: vi.fn(() => of(CREATED)),
    };
    notifications = { success: vi.fn(), error: vi.fn() };
  });

  async function setup() {
    await TestBed.configureTestingModule({
      imports: [CompanyListComponent],
      providers: [
        { provide: ORGANIZATION_SERVICE, useValue: organizations },
        { provide: NotificationService, useValue: notifications },
        {
          provide: AuthStore,
          useValue: { isAdmin: () => true, hasRole: () => true },
        },
        {
          provide: COMPANY_ADDRESS_SERVICE,
          useValue: { list: () => of([]), create: () => of(null), update: () => of(null) },
        },
        {
          provide: GEOGRAPHY_SERVICE,
          useValue: { listCountries: () => of([]), searchCities: () => of({ items: [] }) },
        },
      ],
    }).compileComponents();
    const fixture = TestBed.createComponent(CompanyListComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    return fixture;
  }

  it('blocks creation until at least one role is selected', async () => {
    const fixture = await setup();
    const component = fixture.componentInstance as unknown as TestableCompanyList;

    component.startCreate();
    fixture.detectChanges();
    component.createForm.controls['legalName'].setValue('Atlantic Fisheries');
    component.submitCreate();
    fixture.detectChanges();

    expect(component.createForm.hasError('atLeastOneRole', 'roles')).toBe(true);
    expect(fixture.nativeElement.textContent).toContain('Select at least one role.');
    expect(organizations.create).not.toHaveBeenCalled();
  });

  it('creates a company with the selected roles', async () => {
    const fixture = await setup();
    const component = fixture.componentInstance as unknown as TestableCompanyList;

    component.startCreate();
    component.createForm.controls['legalName'].setValue('Atlantic Fisheries');
    component.createForm.controls['countryCode'].setValue('MA');
    (component.createForm.controls['roles'] as FormGroup).get('SUPPLIER')?.setValue(true);
    component.submitCreate();

    expect(organizations.create).toHaveBeenCalledWith(
      expect.objectContaining({
        legalName: 'Atlantic Fisheries',
        countryCode: 'MA',
        roles: ['SUPPLIER'],
      }),
    );
    expect(notifications.success).toHaveBeenCalled();
  });

  it('edits an unconfigured organization without forcing or clearing roles', async () => {
    const fixture = await setup();
    const component = fixture.componentInstance as unknown as TestableCompanyList;

    component.startEdit(UNCONFIGURED);
    expect(component.editForm.controls['roles'].valid).toBe(true);

    component.editForm.controls['legalName'].setValue('Legacy Importer SARL');
    component.submitEdit();

    expect(organizations.update).toHaveBeenCalledTimes(1);
    const [, patch] = organizations.update.mock.calls[0];
    expect(patch).toEqual(expect.objectContaining({ expectedVersion: UNCONFIGURED.version }));
    expect(Object.prototype.hasOwnProperty.call(patch, 'roles')).toBe(false);
  });
});
