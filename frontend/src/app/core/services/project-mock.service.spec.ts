import { firstValueFrom } from 'rxjs';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { ApiProblemError, CreateProjectRequest } from '../models/project.model';
import { ProjectMockService } from './project-mock.service';

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

function validRequest(overrides: Partial<CreateProjectRequest> = {}): CreateProjectRequest {
  return {
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
    notes: 'Priority shipment.',
    ...overrides,
  };
}

describe('ProjectMockService', () => {
  let service: ProjectMockService;

  beforeEach(() => {
    localStorage.clear();
    service = new ProjectMockService(0);
  });

  afterEach(() => {
    localStorage.clear();
  });

  async function seed(references: string[]): Promise<void> {
    for (const reference of references) {
      await firstValueFrom(service.create(validRequest({ reference })));
    }
  }

  it('starts empty until projects are created', async () => {
    const page = await firstValueFrom(service.list());
    expect(page.items).toEqual([]);
    expect(page.totalElements).toBe(0);
  });

  it('lists the created projects', async () => {
    await seed(['EXP-2026-0001', 'EXP-2026-0002']);
    const page = await firstValueFrom(service.list());
    expect(page.items.length).toBe(2);
    expect(page.page).toBe(0);
    expect(page.size).toBe(20);
    expect(page.totalElements).toBe(2);
  });

  it('paginates results', async () => {
    await seed(['EXP-2026-0001', 'EXP-2026-0002', 'EXP-2026-0003', 'EXP-2026-0004']);
    const page = await firstValueFrom(service.list({ page: 0, size: 3 }));
    expect(page.items.length).toBe(3);
    expect(page.totalPages).toBe(2);
  });

  it('creates a draft project with contract defaults', async () => {
    const created = await firstValueFrom(service.create(validRequest()));

    expect(created.reference).toBe('EXP-2026-0042');
    expect(created.status).toBe('DRAFT_DATA');
    expect(created.version).toBe(0);
    expect(created.producerIds).toEqual([ORG.producer]);
    expect(created.id).toBeTruthy();
  });

  it('normalizes the reference to trimmed uppercase', async () => {
    const created = await firstValueFrom(
      service.create(validRequest({ reference: ' exp-2026-0042 ' })),
    );
    expect(created.reference).toBe('EXP-2026-0042');
  });

  it('rejects a duplicate reference with a 409 problem', async () => {
    await firstValueFrom(service.create(validRequest({ reference: 'EXP-2026-0500' })));
    await expect(
      firstValueFrom(service.create(validRequest({ reference: 'exp-2026-0500' }))),
    ).rejects.toMatchObject({ status: 409 });
  });

  it('filters by reference prefix (q)', async () => {
    await seed(['EXP-2026-0001', 'EXP-2026-0002']);
    const page = await firstValueFrom(service.list({ q: 'EXP-2026-0001' }));
    expect(page.items.length).toBe(1);
    expect(page.items.every((project) => project.reference.startsWith('EXP-2026-0001'))).toBe(true);
  });

  it('filters by status', async () => {
    await seed(['EXP-2026-0001', 'EXP-2026-0002']);
    const drafts = await firstValueFrom(service.list({ status: 'DRAFT_DATA' }));
    expect(drafts.items.length).toBe(2);
    const complete = await firstValueFrom(service.list({ status: 'COMPLETE' }));
    expect(complete.items).toEqual([]);
  });

  it('retrieves a project by id and rejects unknown ids', async () => {
    await seed(['EXP-2026-0001']);
    const page = await firstValueFrom(service.list());
    const first = page.items[0];
    const fetched = await firstValueFrom(service.get(first.id));
    expect(fetched.id).toBe(first.id);

    await expect(firstValueFrom(service.get('missing-id'))).rejects.toBeInstanceOf(ApiProblemError);
  });

  it('updates a project with a matching expectedVersion', async () => {
    await seed(['EXP-2026-0001']);
    const page = await firstValueFrom(service.list());
    const first = page.items[0];
    const updated = await firstValueFrom(
      service.update(first.id, { expectedVersion: first.version, notes: 'Updated note' }),
    );
    expect(updated.notes).toBe('Updated note');
    expect(updated.version).toBe(first.version + 1);
  });

  it('rejects a stale expectedVersion with a 409 problem', async () => {
    await seed(['EXP-2026-0001']);
    const page = await firstValueFrom(service.list());
    const first = page.items[0];
    await expect(
      firstValueFrom(service.update(first.id, { expectedVersion: first.version + 99 })),
    ).rejects.toMatchObject({ status: 409 });
  });

  it('persists created projects to storage', async () => {
    await firstValueFrom(service.create(validRequest({ reference: 'EXP-2026-0099' })));
    const reloaded = new ProjectMockService(0);
    const page = await firstValueFrom(reloaded.list({ size: 100 }));
    expect(page.items.some((project) => project.reference === 'EXP-2026-0099')).toBe(true);
  });
});
