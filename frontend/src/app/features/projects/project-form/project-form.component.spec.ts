import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { Country, Organization, Port } from '../../../core/models/directory.model';
import { CreateProjectRequest, Project } from '../../../core/models/project.model';
import { DIRECTORY_SERVICE } from '../../../core/services/directory.service';
import { NotificationService } from '../../../core/services/notification.service';
import { ProjectStore } from '../../../core/services/project.store';
import { ProjectFormComponent } from './project-form.component';

const ORG = {
  exporter: '00000000-0000-4000-8000-000000000101',
  producer: '00000000-0000-4000-8000-000000000102',
  consignee: '00000000-0000-4000-8000-000000000201',
  forwarder: '00000000-0000-4000-8000-000000000401',
  line: '00000000-0000-4000-8000-000000000501',
};

const PORT = {
  loading: '00000000-0000-4000-8000-000000000601',
  discharge: '00000000-0000-4000-8000-000000000701',
};

const ORGANIZATIONS: Organization[] = [
  { id: ORG.exporter, legalName: 'Exporter', countryCode: 'MA', roles: ['EXPORTER'], active: true },
  { id: ORG.producer, legalName: 'Producer', countryCode: 'MA', roles: ['PRODUCER'], active: true },
  {
    id: ORG.consignee,
    legalName: 'Consignee',
    countryCode: 'ES',
    roles: ['CONSIGNEE'],
    active: true,
  },
  {
    id: ORG.forwarder,
    legalName: 'Forwarder',
    countryCode: 'MA',
    roles: ['FREIGHT_FORWARDER'],
    active: true,
  },
  { id: ORG.line, legalName: 'Line', countryCode: 'DK', roles: ['SHIPPING_LINE'], active: true },
];

const PORTS: Port[] = [
  { id: PORT.loading, name: 'Tanger Med', countryCode: 'MA', unLocode: 'MAPTM', active: true },
  { id: PORT.discharge, name: 'Vigo', countryCode: 'ES', unLocode: 'ESVGO', active: true },
];

const COUNTRIES: Country[] = [{ code: 'ES', name: 'Spain' }];

const CREATED: Project = {
  id: 'new-id',
  version: 0,
  status: 'DRAFT_DATA',
  reference: 'EXP-2026-0042',
  exporterId: ORG.exporter,
  producerIds: [ORG.producer],
  consigneeId: ORG.consignee,
  notifyPartyId: ORG.consignee,
  freightForwarderId: ORG.forwarder,
  shippingLineId: ORG.line,
  destinationCountryCode: 'ES',
  portOfLoadingId: PORT.loading,
  portOfDischargeId: PORT.discharge,
  expectedDepartureDate: '2027-01-15',
  notes: null,
  createdAt: new Date().toISOString(),
  updatedAt: new Date().toISOString(),
};

function storeStub() {
  return {
    projects: signal<Project[]>([]),
    loading: signal(false),
    error: signal<string | null>(null),
    loaded: signal(true),
    page: signal({ items: [], page: 0, size: 20, totalElements: 0, totalPages: 0 }),
    query: signal({ page: 0, size: 20 }),
    stats: signal({}),
    recentProjects: signal<Project[]>([]),
    upcomingDepartures: signal<Project[]>([]),
    loadPage: vi.fn(),
    loadDashboard: vi.fn(),
    createProject: vi.fn((_input: CreateProjectRequest) => of(CREATED)),
    getProject: vi.fn(),
    updateProject: vi.fn(),
  };
}

const directoryStub = {
  organizations: (role?: string) =>
    of(role ? ORGANIZATIONS.filter((org) => org.roles.includes(role as never)) : ORGANIZATIONS),
  ports: () => of(PORTS),
  countries: () => of(COUNTRIES),
};

describe('ProjectFormComponent', () => {
  let store: ReturnType<typeof storeStub>;
  let notifications: {
    success: ReturnType<typeof vi.fn>;
    error: ReturnType<typeof vi.fn>;
    info: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    store = storeStub();
    notifications = { success: vi.fn(), error: vi.fn(), info: vi.fn() };

    await TestBed.configureTestingModule({
      imports: [ProjectFormComponent],
      providers: [
        provideRouter([]),
        { provide: ProjectStore, useValue: store },
        { provide: DIRECTORY_SERVICE, useValue: directoryStub },
        { provide: NotificationService, useValue: notifications },
      ],
    }).compileComponents();
  });

  function create() {
    const fixture = TestBed.createComponent(ProjectFormComponent);
    fixture.detectChanges();
    return fixture;
  }

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  function form(fixture: ReturnType<typeof create>): any {
    return (fixture.componentInstance as any).form;
  }

  function fillValid(fixture: ReturnType<typeof create>): void {
    form(fixture).patchValue({
      reference: 'EXP-2026-0042',
      exporterId: ORG.exporter,
      producerIds: [ORG.producer],
      consigneeId: ORG.consignee,
      notifyPartyId: ORG.consignee,
      freightForwarderId: ORG.forwarder,
      shippingLineId: ORG.line,
      destinationCountryCode: 'ES',
      portOfLoadingId: PORT.loading,
      portOfDischargeId: PORT.discharge,
      expectedDepartureDate: '2027-01-15',
      notes: '',
    });
  }

  it('accepts a reference-only draft', () => {
    const fixture = create();
    form(fixture).controls.reference.setValue('DRAFT-ONLY');
    expect(form(fixture).valid).toBe(true);
  });

  it('rejects a malformed project reference', () => {
    const fixture = create();
    const control = form(fixture).controls.reference;
    control.setValue('has spaces');
    expect(control.hasError('pattern')).toBe(true);
  });

  it('accepts a historical ETD (contract permits past dates)', () => {
    const fixture = create();
    const control = form(fixture).controls.expectedDepartureDate;
    const yesterday = new Date(Date.now() - 86_400_000).toISOString().slice(0, 10);
    control.setValue(yesterday);
    expect(control.hasError('pastDate')).toBe(false);
  });

  it('does not submit an invalid form', () => {
    const fixture = create();
    form(fixture).controls.reference.setValue('');
    (fixture.componentInstance as unknown as { submit(): void }).submit();
    expect(store.createProject).not.toHaveBeenCalled();
    expect(notifications.error).toHaveBeenCalled();
  });

  it('submits a valid form and navigates to the new project', () => {
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    const fixture = create();
    fillValid(fixture);

    (fixture.componentInstance as unknown as { submit(): void }).submit();

    expect(store.createProject).toHaveBeenCalledTimes(1);
    const payload = store.createProject.mock.calls[0][0];
    expect(payload.reference).toBe('EXP-2026-0042');
    expect(payload.consigneeId).toBe(ORG.consignee);
    expect(navigate).toHaveBeenCalledWith(['/projects', CREATED.id]);
  });

  it('copies the consignee id into the notify party when requested', () => {
    const fixture = create();
    const control = form(fixture).controls;
    control.consigneeId.setValue(ORG.consignee);

    (
      fixture.componentInstance as unknown as { onNotifyToggle(checked: boolean): void }
    ).onNotifyToggle(true);

    expect(control.notifyPartyId.getRawValue()).toBe(ORG.consignee);
    expect(control.notifyPartyId.disabled).toBe(true);
  });
});
