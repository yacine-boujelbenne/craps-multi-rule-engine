import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { CreateProjectRequest, Project } from '../../../core/models/project.model';
import { NotificationService } from '../../../core/services/notification.service';
import { ProjectStore } from '../../../core/services/project.store';
import { ProjectFormComponent } from './project-form.component';

const CREATED: Project = {
  id: 'new-id',
  reference: 'EXP-2026-0042',
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
  createdAt: new Date().toISOString(),
  updatedAt: new Date().toISOString(),
};

function storeStub() {
  return {
    projects: signal<Project[]>([]),
    loading: signal(false),
    error: signal<string | null>(null),
    loaded: signal(true),
    stats: signal({}),
    recentProjects: signal<Project[]>([]),
    upcomingDepartures: signal<Project[]>([]),
    loadProjects: vi.fn(),
    createProject: vi.fn((_input: CreateProjectRequest) => of(CREATED)),
    getProject: vi.fn(),
    updateProject: vi.fn(),
  };
}

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
    const f = form(fixture);
    f.patchValue({
      reference: 'EXP-2026-0042',
      exporter: { name: 'Atlantic Blue Seafood Co.', country: 'Morocco' },
      producer: { name: 'Nordic Catch Processing', country: 'Morocco' },
      consignee: { name: 'Iberia Import S.L.', country: 'Spain' },
      notifyParty: { name: 'Iberia Import S.L.', country: 'Spain' },
      freightForwarder: { name: 'TransGlobal Logistics', country: 'Morocco' },
      shippingLine: { name: 'Maersk Line' },
      destination: 'Spain — Vigo',
      loadingPort: 'Tanger Med',
      dischargePort: 'Vigo',
      etd: '2027-01-15',
    });
  }

  it('is invalid until all required fields are provided', () => {
    const fixture = create();
    expect(form(fixture).valid).toBe(false);
  });

  it('rejects a malformed project reference', () => {
    const fixture = create();
    const f = form(fixture);
    f.controls.reference.setValue('bad-ref');
    expect(f.controls.reference.hasError('pattern')).toBe(true);
  });

  it('rejects an ETD in the past', () => {
    const fixture = create();
    const f = form(fixture);
    const yesterday = new Date(Date.now() - 86_400_000).toISOString().slice(0, 10);
    f.controls.etd.setValue(yesterday);
    expect(f.controls.etd.hasError('pastDate')).toBe(true);
  });

  it('does not submit an invalid form', () => {
    const fixture = create();
    (fixture.componentInstance as any).submit();
    expect(store.createProject).not.toHaveBeenCalled();
    expect(notifications.error).toHaveBeenCalled();
  });

  it('submits a valid form and navigates to the new project', () => {
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    const fixture = create();
    fillValid(fixture);

    (fixture.componentInstance as any).submit();

    expect(store.createProject).toHaveBeenCalledTimes(1);
    const payload = store.createProject.mock.calls[0][0];
    expect(payload.reference).toBe('EXP-2026-0042');
    expect(payload.consignee.name).toBe('Iberia Import S.L.');
    expect(navigate).toHaveBeenCalledWith(['/projects', CREATED.id]);
  });

  it('copies the consignee into the notify party when requested', () => {
    const fixture = create();
    const f = form(fixture);
    f.controls.consignee.setValue({ name: 'Iberia Import S.L.', country: 'Spain' });

    (fixture.componentInstance as any).onNotifyToggle(true);

    expect(f.controls.notifyParty.getRawValue()).toEqual({
      name: 'Iberia Import S.L.',
      country: 'Spain',
    });
    expect(f.controls.notifyParty.disabled).toBe(true);
  });
});
