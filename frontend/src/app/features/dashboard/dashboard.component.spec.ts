import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { Country, Organization, Port } from '../../core/models/directory.model';
import { DashboardStats, Project } from '../../core/models/project.model';
import { AUTH_SERVICE } from '../../core/services/auth.service';
import { AuthMockService } from '../../core/services/auth-mock.service';
import { DIRECTORY_SERVICE } from '../../core/services/directory.service';
import { MOCK_LATENCY_MS } from '../../core/services/project-mock.service';
import { ProjectStore } from '../../core/services/project.store';
import { DashboardComponent } from './dashboard.component';

const ORG = {
  exporter: '00000000-0000-4000-8000-000000000101',
  producer: '00000000-0000-4000-8000-000000000102',
  consignee: '00000000-0000-4000-8000-000000000201',
};

const ORGANIZATIONS: Organization[] = [
  {
    id: ORG.consignee,
    legalName: 'Iberia Import S.L.',
    countryCode: 'ES',
    roles: ['CONSIGNEE'],
    active: true,
  },
];

const COUNTRIES: Country[] = [
  { code: 'ES', name: 'Spain' },
  { code: 'IT', name: 'Italy' },
];

const directoryStub = {
  organizations: () => of(ORGANIZATIONS),
  ports: () => of([] as Port[]),
  countries: () => of(COUNTRIES),
};

function project(overrides: Partial<Project>): Project {
  return {
    id: 'id',
    version: 0,
    status: 'DRAFT_DATA',
    reference: 'EXP-2026-0001',
    exporterId: ORG.exporter,
    producerIds: [ORG.producer],
    consigneeId: ORG.consignee,
    notifyPartyId: null,
    freightForwarderId: null,
    shippingLineId: null,
    destinationCountryCode: 'ES',
    portOfLoadingId: null,
    portOfDischargeId: null,
    expectedDepartureDate: '2027-01-15',
    notes: null,
    createdAt: '2026-01-01T00:00:00.000Z',
    updatedAt: '2026-01-01T00:00:00.000Z',
    ...overrides,
  };
}

describe('DashboardComponent', () => {
  async function setup() {
    const stats: DashboardStats = {
      totalProjects: 10,
      draftProjects: 3,
      awaitingValidation: 2,
      inProgress: 2,
      completed: 3,
    };
    const recent = [project({ id: 'r1', reference: 'EXP-2026-0010' })];
    const upcoming = [
      project({ id: 'u1', reference: 'EXP-2026-0011', destinationCountryCode: 'IT' }),
    ];

    const store = {
      projects: signal<Project[]>([...recent, ...upcoming]),
      loading: signal(false),
      error: signal<string | null>(null),
      loaded: signal(true),
      page: signal({ items: [], page: 0, size: 20, totalElements: 0, totalPages: 0 }),
      query: signal({ page: 0, size: 20 }),
      stats: signal(stats),
      recentProjects: signal(recent),
      upcomingDepartures: signal(upcoming),
      loadPage: vi.fn(),
      loadDashboard: vi.fn(),
      createProject: vi.fn(),
      getProject: vi.fn(),
      updateProject: vi.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [DashboardComponent],
      providers: [
        provideRouter([]),
        { provide: ProjectStore, useValue: store },
        { provide: DIRECTORY_SERVICE, useValue: directoryStub },
        { provide: MOCK_LATENCY_MS, useValue: 0 },
        { provide: AUTH_SERVICE, useClass: AuthMockService },
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(DashboardComponent);
    fixture.detectChanges();
    return fixture;
  }

  it('renders the key figures', async () => {
    const fixture = await setup();
    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Total projects');
    expect(text).toContain('10');
    expect(text).toContain('Awaiting validation');
    expect(text).toContain('2');
  });

  it('lists recent projects and upcoming departures', async () => {
    const fixture = await setup();
    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('EXP-2026-0010');
    expect(text).toContain('EXP-2026-0011');
    expect(text).toContain('Italy');
  });

  it('loads the dashboard on init', async () => {
    await setup();
    const store = TestBed.inject(ProjectStore) as unknown as {
      loadDashboard: ReturnType<typeof vi.fn>;
    };
    expect(store.loadDashboard).toHaveBeenCalled();
  });
});
