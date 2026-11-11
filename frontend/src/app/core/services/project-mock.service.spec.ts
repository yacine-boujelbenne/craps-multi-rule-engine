import { firstValueFrom } from 'rxjs';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { CreateProjectRequest } from '../models/project.model';
import { ProjectMockService } from './project-mock.service';

function validRequest(overrides: Partial<CreateProjectRequest> = {}): CreateProjectRequest {
  return {
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
    ...overrides,
  };
}

describe('ProjectMockService', () => {
  let service: ProjectMockService;

  beforeEach(() => {
    localStorage.clear();
    service = new ProjectMockService();
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('lists seeded projects', async () => {
    const projects = await firstValueFrom(service.list());
    expect(projects.length).toBeGreaterThan(0);
  });

  it('creates a draft project with the full document workflow', async () => {
    const created = await firstValueFrom(service.create(validRequest()));

    expect(created.reference).toBe('EXP-2026-0042');
    expect(created.status).toBe('DRAFT');
    expect(created.documentSteps).toHaveLength(8);
    expect(created.documentSteps.every((step) => step.status === 'NOT_STARTED')).toBe(true);
    expect(created.id).toBeTruthy();
  });

  it('generates a reference when none is provided', async () => {
    const created = await firstValueFrom(service.create(validRequest({ reference: '' })));
    expect(created.reference).toMatch(/^EXP-\d{4}-\d{4}$/);
  });

  it('filters by search term', async () => {
    const results = await firstValueFrom(service.list({ search: 'vigo' }));
    expect(results.length).toBeGreaterThan(0);
    expect(
      results.every((project) =>
        `${project.reference} ${project.consignee.name} ${project.destination}`
          .toLowerCase()
          .includes('vigo'),
      ),
    ).toBe(true);
  });

  it('filters by status', async () => {
    const results = await firstValueFrom(service.list({ status: 'DRAFT' }));
    expect(results.every((project) => project.status === 'DRAFT')).toBe(true);
  });

  it('retrieves a project by id and rejects unknown ids', async () => {
    const [first] = await firstValueFrom(service.list());
    const fetched = await firstValueFrom(service.get(first.id));
    expect(fetched.id).toBe(first.id);

    await expect(firstValueFrom(service.get('missing-id'))).rejects.toThrow();
  });

  it('updates a project', async () => {
    const [first] = await firstValueFrom(service.list());
    const updated = await firstValueFrom(service.update(first.id, { status: 'COMPLETE' }));
    expect(updated.status).toBe('COMPLETE');
  });

  it('persists created projects to storage', async () => {
    await firstValueFrom(service.create(validRequest({ reference: 'EXP-2026-0099' })));
    const reloaded = new ProjectMockService();
    const projects = await firstValueFrom(reloaded.list());
    expect(projects.some((project) => project.reference === 'EXP-2026-0099')).toBe(true);
  });
});
