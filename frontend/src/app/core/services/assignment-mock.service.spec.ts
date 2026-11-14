import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { beforeEach, describe, expect, it } from 'vitest';
import { FORM_KEYS } from '../models/account.model';
import { AssignmentMockService } from './assignment-mock.service';
import { AuthMockService, MOCK_ACCOUNTS_SEED } from './auth-mock.service';
import { AUTH_SERVICE } from './auth.service';
import { AuthStore } from './auth.store';
import { MOCK_LATENCY_MS } from './project-mock.service';
import { TEST_ACCOUNTS } from '../testing/mock-accounts';

const PROJECT_ID = '11111111-1111-4111-8111-000000000001';
const OPERATOR_ID = '00000000-0000-4000-8000-000000000002';

describe('AssignmentMockService', () => {
  let service: AssignmentMockService;
  let auth: AuthStore;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        { provide: MOCK_LATENCY_MS, useValue: 0 },
        { provide: MOCK_ACCOUNTS_SEED, useValue: TEST_ACCOUNTS },
        { provide: AUTH_SERVICE, useClass: AuthMockService },
        AssignmentMockService,
      ],
    });
    service = TestBed.inject(AssignmentMockService);
    auth = TestBed.inject(AuthStore);
  });

  it('grants an admin every form and assignment management', async () => {
    await firstValueFrom(auth.login({ username: 'admin', password: 'Admin-Passw0rd' }));
    const permissions = await firstValueFrom(service.permissions(PROJECT_ID));
    expect(permissions.canManageAssignments).toBe(true);
    expect(permissions.editableForms).toEqual(FORM_KEYS);
  });

  it('reports read-only access for an unassigned operator', async () => {
    await firstValueFrom(auth.login({ username: 'operator', password: 'Operator-Passw0rd' }));
    const permissions = await firstValueFrom(service.permissions(PROJECT_ID));
    expect(permissions.canRead).toBe(true);
    expect(permissions.canManageAssignments).toBe(false);
    expect(permissions.editableForms).toEqual([]);
  });

  it('reflects assigned form keys in the effective permissions', async () => {
    await firstValueFrom(auth.login({ username: 'operator', password: 'Operator-Passw0rd' }));
    await firstValueFrom(service.assign(PROJECT_ID, OPERATOR_ID, ['INVOICE', 'PACKING_LIST']));
    const permissions = await firstValueFrom(service.permissions(PROJECT_ID));
    expect(permissions.editableForms).toEqual(['INVOICE', 'PACKING_LIST']);
  });

  it('removes a grant on unassign', async () => {
    await firstValueFrom(auth.login({ username: 'operator', password: 'Operator-Passw0rd' }));
    await firstValueFrom(service.assign(PROJECT_ID, OPERATOR_ID, ['INVOICE']));
    await firstValueFrom(service.unassign(PROJECT_ID, OPERATOR_ID));
    const permissions = await firstValueFrom(service.permissions(PROJECT_ID));
    expect(permissions.editableForms).toEqual([]);
  });
});
