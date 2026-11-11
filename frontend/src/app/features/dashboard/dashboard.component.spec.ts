import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { DashboardStats, Project } from '../../core/models/project.model';
import { ProjectStore } from '../../core/services/project.store';
import { DashboardComponent } from './dashboard.component';

function project(overrides: Partial<Project>): Project {
  return {
    id: 'id',
    reference: 'EXP-2026-0001',
    exporter: { name: 'Exporter' },
    producer: { name: 'Producer' },
    consignee: { name: 'Iberia Import S.L.' },
    notifyParty: { name: 'Iberia Import S.L.' },
    freightForwarder: { name: 'Forwarder' },
    shippingLine: { name: 'Line' },
    destination: 'Spain — Vigo',
    loadingPort: 'Tanger Med',
    dischargePort: 'Vigo',
    etd: '2027-01-15',
    status: 'DRAFT',
    documentSteps: [],
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
      project({ id: 'u1', reference: 'EXP-2026-0011', destination: 'Italy — Genoa' }),
    ];

    const store = {
      projects: signal<Project[]>([...recent, ...upcoming]),
      loading: signal(false),
      error: signal<string | null>(null),
      loaded: signal(true),
      stats: signal(stats),
      recentProjects: signal(recent),
      upcomingDepartures: signal(upcoming),
      loadProjects: vi.fn(),
      createProject: vi.fn(),
      getProject: vi.fn(),
      updateProject: vi.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [DashboardComponent],
      providers: [provideRouter([]), { provide: ProjectStore, useValue: store }],
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
    expect(text).toContain('Italy — Genoa');
  });

  it('loads projects on init', async () => {
    await setup();
    const store = TestBed.inject(ProjectStore) as unknown as {
      loadProjects: ReturnType<typeof vi.fn>;
    };
    expect(store.loadProjects).toHaveBeenCalled();
  });
});
