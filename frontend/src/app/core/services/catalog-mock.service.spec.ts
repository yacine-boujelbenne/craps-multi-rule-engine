import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { beforeEach, describe, expect, it } from 'vitest';
import { ApiProblemError } from '../models/project.model';
import { CatalogMockService } from './catalog-mock.service';
import { MOCK_LATENCY_MS } from './project-mock.service';

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
