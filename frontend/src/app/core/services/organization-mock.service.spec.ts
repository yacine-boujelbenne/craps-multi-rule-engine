import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { beforeEach, describe, expect, it } from 'vitest';
import { ApiProblemError } from '../models/project.model';
import { OrganizationMockService } from './organization-mock.service';
import { MOCK_LATENCY_MS } from './project-mock.service';

describe('OrganizationMockService', () => {
  let service: OrganizationMockService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [{ provide: MOCK_LATENCY_MS, useValue: 0 }, OrganizationMockService],
    });
    service = TestBed.inject(OrganizationMockService);
  });

  it('creates an active organization at version zero and trims the legal name', async () => {
    const organization = await firstValueFrom(
      service.create({
        legalName: '  Atlantic Fisheries  ',
        countryCode: 'MA',
        roles: ['SUPPLIER', 'PRODUCER'],
        active: true,
      }),
    );
    expect(organization.legalName).toBe('Atlantic Fisheries');
    expect(organization.version).toBe(0);
    expect(organization.active).toBe(true);
    expect(organization.roles).toEqual(['SUPPLIER', 'PRODUCER']);
  });

  it('rejects an organization without roles', async () => {
    await expect(
      firstValueFrom(service.create({ legalName: 'No Roles Ltd', roles: [], active: true })),
    ).rejects.toBeInstanceOf(ApiProblemError);
  });

  it('lists active organizations by default and includes inactive on request', async () => {
    const active = await firstValueFrom(
      service.create({ legalName: 'Active Co', roles: ['CLIENT'], active: true }),
    );
    await firstValueFrom(
      service.create({ legalName: 'Dormant Co', roles: ['CLIENT'], active: false }),
    );

    const page = await firstValueFrom(service.list());
    expect(page.items.map((item) => item.id)).toEqual([active.id]);

    const all = await firstValueFrom(service.list({ includeInactive: true }));
    expect(all.items.map((item) => item.legalName)).toEqual(['Active Co', 'Dormant Co']);
  });

  it('filters the directory by role', async () => {
    await firstValueFrom(
      service.create({ legalName: 'Supplier Co', roles: ['SUPPLIER'], active: true }),
    );
    const importer = await firstValueFrom(
      service.create({ legalName: 'Importer Co', roles: ['IMPORTER'], active: true }),
    );

    const page = await firstValueFrom(service.list({ role: 'IMPORTER' }));
    expect(page.items.map((item) => item.id)).toEqual([importer.id]);
  });

  it('rejects a stale update and keeps the version on a no-op', async () => {
    const organization = await firstValueFrom(
      service.create({ legalName: 'Versioned Co', roles: ['BUYER'], active: true }),
    );

    const updated = await firstValueFrom(
      service.update(organization.id, { expectedVersion: 0, legalName: 'Versioned Co SARL' }),
    );
    expect(updated.version).toBe(1);

    const noop = await firstValueFrom(
      service.update(organization.id, { expectedVersion: 1, legalName: 'Versioned Co SARL' }),
    );
    expect(noop.version).toBe(1);

    await expect(
      firstValueFrom(service.update(organization.id, { expectedVersion: 0, active: false })),
    ).rejects.toBeInstanceOf(ApiProblemError);
  });
});
