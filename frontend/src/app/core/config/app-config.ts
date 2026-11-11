/**
 * Runtime application configuration.
 *
 * `useMockApi` keeps the UI fully functional before the backend OpenAPI
 * contract is published. Switch it to `false` (or provide the real
 * `ProjectService`) once Agent 1 freezes the API.
 */
export const APP_CONFIG = {
  apiBaseUrl: '/api',
  useMockApi: true,
} as const;
