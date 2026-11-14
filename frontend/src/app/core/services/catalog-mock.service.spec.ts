import { TestBed } from '@angular/core/testing';
import { firstValueFrom, of } from 'rxjs';
import { beforeEach, describe, expect, it } from 'vitest';
import { ApiProblemError } from '../models/project.model';
import { CatalogMockService } from './catalog-mock.service';
import { MOCK_LATENCY_MS } from './project-mock.service';
import { TAXONOMY_SERVICE, TaxonomyService } from './taxonomy.service';

describe('CatalogMockService', () => {
  let service: CatalogMockService;
  let productId: string;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      providers: [{ provide: MOCK_LATENCY_MS, useValue: 0 }, CatalogMockService],
    });
    service = TestBed.inject(CatalogMockService);
    const product = await firstValueFrom(
      service.createProduct({ code: 'SHR', name: 'Shrimp', active: true }),
    );
    productId = product.id;
  });

  it('creates a product at version zero and rejects a duplicate code', async () => {
    await expect(
      firstValueFrom(service.createProduct({ code: 'SHR', name: 'Other shrimp', active: true })),
    ).rejects.toBeInstanceOf(ApiProblemError);
  });

  it('filters the product list by taxonomy category', async () => {
    let assignedId = '';
    const category = {
      id: 'category-1',
      code: 'FISH',
      name: 'Fish',
      version: 0,
      active: true,
      description: null,
      createdAt: '2026-01-01T00:00:00.000Z',
      updatedAt: '2026-01-01T00:00:00.000Z',
    };
    const taxonomy = {
      listAssignments: (id: string) => of(id === assignedId ? [category] : []),
    } as unknown as TaxonomyService;

    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [
        { provide: MOCK_LATENCY_MS, useValue: 0 },
        { provide: TAXONOMY_SERVICE, useValue: taxonomy },
        CatalogMockService,
      ],
    });
    const filteredService = TestBed.inject(CatalogMockService);
    await firstValueFrom(
      filteredService.createProduct({ code: 'SHR', name: 'Shrimp', active: true }),
    );
    const assigned = await firstValueFrom(
      filteredService.createProduct({ code: 'TUN', name: 'Tuna', active: true }),
    );
    assignedId = assigned.id;

    const page = await firstValueFrom(filteredService.listProducts({ categoryId: 'category-1' }));
    expect(page.items.map((product) => product.code)).toEqual(['TUN']);
  });

  it('enforces the price/currency pairing rule on grades', async () => {
    await expect(
      firstValueFrom(
        service.createGrade(productId, {
          code: 'G1',
          name: 'Grade 1',
          displayOrder: 1,
          defaultPricePerKg: '9.5000',
          active: true,
        }),
      ),
    ).rejects.toBeInstanceOf(ApiProblemError);

    const grade = await firstValueFrom(
      service.createGrade(productId, {
        code: 'G1',
        name: 'Grade 1',
        displayOrder: 1,
        defaultPricePerKg: '9.5000',
        defaultCurrency: 'EUR',
        active: true,
      }),
    );
    expect(grade.defaultCurrency).toBe('EUR');
  });

  it('orders grades by display order then code', async () => {
    await firstValueFrom(
      service.createGrade(productId, { code: 'B', name: 'B', displayOrder: 2, active: true }),
    );
    await firstValueFrom(
      service.createGrade(productId, { code: 'A', name: 'A', displayOrder: 2, active: true }),
    );
    await firstValueFrom(
      service.createGrade(productId, { code: 'C', name: 'C', displayOrder: 1, active: true }),
    );

    const page = await firstValueFrom(service.listGrades(productId));
    expect(page.items.map((grade) => grade.code)).toEqual(['C', 'A', 'B']);
  });

  it('bumps the grade version on change and rejects a stale update', async () => {
    const grade = await firstValueFrom(
      service.createGrade(productId, {
        code: 'G1',
        name: 'Grade 1',
        displayOrder: 0,
        active: true,
      }),
    );
    const updated = await firstValueFrom(
      service.updateGrade(productId, grade.id, { expectedVersion: 0, displayOrder: 3 }),
    );
    expect(updated.version).toBe(1);

    await expect(
      firstValueFrom(
        service.updateGrade(productId, grade.id, { expectedVersion: 0, displayOrder: 4 }),
      ),
    ).rejects.toBeInstanceOf(ApiProblemError);
  });
});
