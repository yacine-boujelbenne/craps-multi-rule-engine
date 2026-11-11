import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { Project } from '../../../core/models/project.model';
import { ProjectStore } from '../../../core/services/project.store';
import { ProjectListComponent } from './project-list.component';

function project(overrides: Partial<Project>): Project {
  return {
    id: 'id',
    reference: 'EXP-2026-0001',
    exporter: { name: 'Exporter' },
    producer: { name: 'Producer' },
    consignee: { name: 'Consignee' },
    notifyParty: { name: 'Consignee' },
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

const PROJECTS: Project[] = [
  project({
    id: 'a',
    reference: 'EXP-2026-0001',
    status: 'DRAFT',
    consignee: { name: 'Iberia Import S.L.' },
    destination: 'Spain — Vigo',
  }),
  project({
    id: 'b',
    reference: 'EXP-2026-0002',
    status: 'COMPLETE',
    consignee: { name: 'Tokyo Marine Foods' },
    destination: 'Japan — Yokohama',
  }),
];

function storeStub(projects: Project[]) {
  return {
    projects: signal(projects),
    loading: signal(false),
    error: signal<string | null>(null),
    loaded: signal(true),
    stats: signal({}),
    recentProjects: signal(projects),
    upcomingDepartures: signal(projects),
    loadProjects: vi.fn(),
    createProject: vi.fn(),
    getProject: vi.fn(),
    updateProject: vi.fn(),
  };
}

describe('ProjectListComponent', () => {
  async function setup(projects: Project[] = PROJECTS) {
    const store = storeStub(projects);
    await TestBed.configureTestingModule({
      imports: [ProjectListComponent],
      providers: [provideRouter([]), { provide: ProjectStore, useValue: store }],
    }).compileComponents();
    const fixture = TestBed.createComponent(ProjectListComponent);
    fixture.detectChanges();
    return fixture;
  }

  it('renders a row per project', async () => {
    const fixture = await setup();
    const rows = fixture.nativeElement.querySelectorAll('tbody tr');
    expect(rows.length).toBe(2);
    expect(fixture.nativeElement.textContent).toContain('EXP-2026-0001');
    expect(fixture.nativeElement.textContent).toContain('Iberia Import S.L.');
  });

  it('filters rows by the search term', async () => {
    const fixture = await setup();
    const input = fixture.nativeElement.querySelector('input[type="search"]') as HTMLInputElement;
    input.value = 'tokyo';
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    const rows = fixture.nativeElement.querySelectorAll('tbody tr');
    expect(rows.length).toBe(1);
    expect(fixture.nativeElement.textContent).toContain('EXP-2026-0002');
  });

  it('filters rows by status', async () => {
    const fixture = await setup();
    const select = fixture.nativeElement.querySelector('select') as HTMLSelectElement;
    select.value = 'COMPLETE';
    select.dispatchEvent(new Event('change'));
    fixture.detectChanges();

    const rows = fixture.nativeElement.querySelectorAll('tbody tr');
    expect(rows.length).toBe(1);
    expect(fixture.nativeElement.textContent).toContain('EXP-2026-0002');
  });

  it('shows an empty state when there are no projects', async () => {
    const fixture = await setup([]);
    expect(fixture.nativeElement.querySelector('app-empty-state')).toBeTruthy();
    expect(fixture.nativeElement.querySelector('tbody')).toBeNull();
  });
});
