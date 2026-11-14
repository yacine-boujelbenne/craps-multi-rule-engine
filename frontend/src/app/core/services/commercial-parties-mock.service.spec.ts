import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { beforeEach, describe, expect, it } from 'vitest';
import { ApiProblemError } from '../models/project.model';
import { CommercialPartiesMockService } from './commercial-parties-mock.service';
import { MOCK_LATENCY_MS } from './project-mock.service';

describe('CommercialPartiesMockService', () => {
  const projectId = '11111111-1111-4111-8111-111111111111';
  let service: CommercialPartiesMockService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [{ provide: MOCK_LATENCY_MS, useValue: 0 }, CommercialPartiesMockService],
    });
    service = TestBed.inject(CommercialPartiesMockService);
  });

  it('returns an empty record at version zero for a new shipment', async () => {
    const parties = await firstValueFrom(service.get(projectId));
    expect(parties).toEqual({
      projectId,
      version: 0,
      supplierId: null,
      clientId: null,
      importerId: null,
      consigneeId: null,
    });
  });

  it('keeps the four slots independent and bumps the version once', async () => {
    const supplierId = 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa';
    const importerId = 'bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb';

    const first = await firstValueFrom(
      service.update(projectId, { expectedVersion: 0, supplierId }),
    );
    expect(first.version).toBe(1);
    expect(first.supplierId).toBe(supplierId);
    expect(first.importerId).toBeNull();

    const second = await firstValueFrom(
      service.update(projectId, { expectedVersion: 1, importerId }),
    );
    expect(second.version).toBe(2);
    expect(second.supplierId).toBe(supplierId);
    expect(second.importerId).toBe(importerId);
  });

  it('does not bump the version for a no-op patch', async () => {
    const clientId = 'cccccccc-cccc-4ccc-8ccc-cccccccccccc';
    await firstValueFrom(service.update(projectId, { expectedVersion: 0, clientId }));

    const noop = await firstValueFrom(service.update(projectId, { expectedVersion: 1, clientId }));
    expect(noop.version).toBe(1);
  });

  it('rejects a stale expectedVersion', async () => {
    await expect(
      firstValueFrom(service.update(projectId, { expectedVersion: 5, supplierId: null })),
    ).rejects.toBeInstanceOf(ApiProblemError);
  });
});
