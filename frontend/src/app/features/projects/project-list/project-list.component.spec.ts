import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { Country, Organization } from '../../../core/models/directory.model';
import { Project, ProjectPage } from '../../../core/models/project.model';
import { DIRECTORY_SERVICE } from '../../../core/services/directory.service';
import { ProjectStore } from '../../../core/services/project.store';
import { ProjectListComponent } from './project-list.component';

const ORG = {
  iberia: '00000000-0000-4000-8000-000000000201',
  tokyo: '00000000-0000-4000-8000-000000000203',
};

const ORGANIZATIONS: Organization[] = [
  {
    id: ORG.iberia,
    legalName: 'Iberia Import S.L.',
    countryCode: 'ES',
    roles: ['CONSIGNEE'],
    active: true,
  },
  {
    id: ORG.tokyo,
    legalName: 'Tokyo Marine Foods',
    countryCode: 'JP',
    roles: ['CONSIGNEE'],
    active: true,
  },
];

const COUNTRIES: Country[] = [
  { code: 'ES', name: 'Spain' },
  { code: 'JP', name: 'Japan' },
];

const directoryStub = {
  organizations: () => of(ORGANIZATIONS),
  ports: () => of([]),
  countries: () => of(COUNTRIES),
};

function project(overrides: Partial<Project>): Project {
  return {
    id: 'id',
    version: 0,
    status: 'DRAFT_DATA',
    reference: 'EXP-2026-0001',
    exporterId: null,
    producerIds: [],
    consigneeId: ORG.iberia,
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

const PROJECTS: Project[] = [
  project({
    id: 'a',
    reference: 'EXP-2026-0001',
    consigneeId: ORG.iberia,
    destinationCountryCode: 'ES',
  }),
  project({
    id: 'b',
    reference: 'EXP-2026-0002',
    status: 'COMPLETE',
    consigneeId: ORG.tokyo,
    destinationCountryCode: 'JP',
  }),
];

function page(items: Project[]): ProjectPage {
  return {
    items,
    page: 0,
    size: 20,
    totalElements: items.length,
    totalPages: items.length ? 1 : 0,
  };
}

function storeStub(items: Project[]) {
  return {
    page: signal(page(items)),
    query: signal({ page: 0, size: 20 }),
    loading: signal(false),
    error: signal<string | null>(null),
    loaded: signal(true),
    projects: signal(items),
    stats: signal({}),
    recentProjects: signal(items),
    upcomingDepartures: signal(items),
    loadPage: vi.fn(),
    loadDashboard: vi.fn(),
    createProject: vi.fn(),
    getProject: vi.fn(),
    updateProject: vi.fn(),
  };
}

describe('ProjectListComponent', () => {
  let store: ReturnType<typeof storeStub>;

  async function setup(items: Project[] = PROJECTS) {
    store = storeStub(items);
    await TestBed.configureTestingModule({
      imports: [ProjectListComponent],
      providers: [
        provideRouter([]),
        { provide: ProjectStore, useValue: store },
        { provide: DIRECTORY_SERVICE, useValue: directoryStub },
      ],
    }).compileComponents();
    const fixture = TestBed.createComponent(ProjectListComponent);
    fixture.detectChanges();
    return fixture;
  }

  beforeEach(() => {
    vi.useRealTimers();
  });

  it('renders a row per project with resolved names', async () => {
    const fixture = await setup();
    const rows = fixture.nativeElement.querySelectorAll('tbody tr');
    expect(rows.length).toBe(2);
    expect(fixture.nativeElement.textContent).toContain('EXP-2026-0001');
    expect(fixture.nativeElement.textContent).toContain('Iberia Import S.L.');
    expect(fixture.nativeElement.textContent).toContain('Japan');
  });

  it('requests a filtered page when searching (debounced)', async () => {
    const fixture = await setup();
    vi.useFakeTimers();
    const component = fixture.componentInstance as unknown as { onSearch(value: string): void };
    component.onSearch('tokyo');
    vi.advanceTimersByTime(300);

    expect(store.loadPage).toHaveBeenLastCalledWith(
      expect.objectContaining({ page: 0, q: 'tokyo' }),
    );
    vi.useRealTimers();
  });

  it('requests a filtered page when the status changes', async () => {
    const fixture = await setup();
    const component = fixture.componentInstance as unknown as {
      onStatusChange(value: string): void;
    };
    component.onStatusChange('COMPLETE');

    expect(store.loadPage).toHaveBeenLastCalledWith(
      expect.objectContaining({ page: 0, status: 'COMPLETE' }),
    );
  });

  it('shows an empty state when there are no projects', async () => {
    const fixture = await setup([]);
    expect(fixture.nativeElement.querySelector('app-empty-state')).toBeTruthy();
    expect(fixture.nativeElement.querySelector('tbody')).toBeNull();
  });
});
