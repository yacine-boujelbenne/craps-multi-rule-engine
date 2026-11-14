import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { beforeEach, describe, expect, it } from 'vitest';
import { AccountMockService } from './account-mock.service';
import { MOCK_ACCOUNTS_SEED } from './auth-mock.service';
import { MOCK_LATENCY_MS } from './project-mock.service';
import { ApiProblemError } from '../models/project.model';
import { TEST_ACCOUNTS } from '../testing/mock-accounts';

describe('AccountMockService', () => {
  let service: AccountMockService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        { provide: MOCK_LATENCY_MS, useValue: 0 },
        { provide: MOCK_ACCOUNTS_SEED, useValue: TEST_ACCOUNTS },
        AccountMockService,
      ],
    });
    service = TestBed.inject(AccountMockService);
  });

  it('lists the seeded accounts ordered by username', async () => {
    const page = await firstValueFrom(service.list());
    expect(page.totalElements).toBe(3);
    expect(page.items.map((user) => user.username)).toEqual(['admin', 'new.user', 'operator']);
  });

  it('creates an account that must change its password', async () => {
    const user = await firstValueFrom(
      service.create({
        username: 'Fresh.User',
        displayName: 'Fresh User',
        roles: ['OPERATOR'],
        initialPassword: 'Initial-Pass1',
      }),
    );
    expect(user.username).toBe('fresh.user');
    expect(user.mustChangePassword).toBe(true);
    expect(user.version).toBe(0);
  });

  it('rejects a duplicate username with a field error', async () => {
    try {
      await firstValueFrom(
        service.create({
          username: 'admin',
          displayName: 'Clone',
          roles: ['VIEWER'],
          initialPassword: 'Initial-Pass1',
        }),
      );
      expect.unreachable('expected the duplicate username to be rejected');
    } catch (error) {
      expect(error).toBeInstanceOf(ApiProblemError);
      expect((error as ApiProblemError).status).toBe(409);
      expect((error as ApiProblemError).messageFor('username')).toBeTruthy();
    }
  });

  it('rejects an update with a stale version', async () => {
    await expect(
      firstValueFrom(
        service.update('00000000-0000-4000-8000-000000000001', { expectedVersion: 99 }),
      ),
    ).rejects.toBeInstanceOf(ApiProblemError);
  });

  it('retains the last active administrator', async () => {
    try {
      await firstValueFrom(
        service.update('00000000-0000-4000-8000-000000000001', {
          expectedVersion: 0,
          active: false,
        }),
      );
      expect.unreachable('expected the last active admin to be retained');
    } catch (error) {
      expect(error).toBeInstanceOf(ApiProblemError);
      expect((error as ApiProblemError).status).toBe(409);
    }
  });

  it('bumps the version and forces a change when resetting a password', async () => {
    const user = await firstValueFrom(
      service.resetPassword('00000000-0000-4000-8000-000000000002', {
        expectedVersion: 0,
        temporaryPassword: 'Temp-Passw0rd1',
      }),
    );
    expect(user.mustChangePassword).toBe(true);
    expect(user.version).toBe(1);
  });
});
