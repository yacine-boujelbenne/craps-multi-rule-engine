/**
 * Runtime application configuration.
 *
 * `useMockApi` selects between the real HTTP services (backend + PostgreSQL)
 * and fictional in-memory fixtures used for offline development and the unit
 * test harness. The default is the real backend.
 */
export const APP_CONFIG = {
  apiBaseUrl: '/api',
  useMockApi: true,
} as const;
