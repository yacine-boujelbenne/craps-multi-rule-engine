import { MockAccount } from '../services/auth-mock.service';

/**
 * Test-only account fixtures. The application seeds a single administrator;
 * specs inject this richer set through `MOCK_ACCOUNTS_SEED` to exercise roles.
 */
export const TEST_ACCOUNTS: MockAccount[] = [
  {
    user: {
      id: '00000000-0000-4000-8000-000000000001',
      username: 'admin',
      displayName: 'Administrator',
      active: true,
      roles: ['ADMIN'],
      mustChangePassword: false,
      version: 0,
    },
    password: 'Admin-Passw0rd',
  },
  {
    user: {
      id: '00000000-0000-4000-8000-000000000002',
      username: 'operator',
      displayName: 'Omar Operator',
      active: true,
      roles: ['OPERATOR'],
      mustChangePassword: false,
      version: 0,
    },
    password: 'Operator-Passw0rd',
  },
  {
    user: {
      id: '00000000-0000-4000-8000-000000000003',
      username: 'new.user',
      displayName: 'New Colleague',
      active: true,
      roles: ['OPERATOR'],
      mustChangePassword: true,
      version: 0,
    },
    password: 'Temporary-Pass1',
  },
];
