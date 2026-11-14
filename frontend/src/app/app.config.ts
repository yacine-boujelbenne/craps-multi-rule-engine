import { provideHttpClient, withInterceptors, withXsrfConfiguration } from '@angular/common/http';
import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter, withInMemoryScrolling } from '@angular/router';
import { provideTranslateService } from '@ngx-translate/core';
import { provideTranslateHttpLoader } from '@ngx-translate/http-loader';
import { routes } from './app.routes';
import { APP_CONFIG } from './core/config/app-config';
import { authErrorInterceptor } from './core/interceptors/auth-error.interceptor';
import { AccountApiService } from './core/services/account-api.service';
import { AccountMockService } from './core/services/account-mock.service';
import { ACCOUNT_SERVICE } from './core/services/account.service';
import { AssignmentApiService } from './core/services/assignment-api.service';
import { AssignmentMockService } from './core/services/assignment-mock.service';
import { ASSIGNMENT_SERVICE } from './core/services/assignment.service';
import { AuthApiService } from './core/services/auth-api.service';
import { AuthMockService } from './core/services/auth-mock.service';
import { AUTH_SERVICE } from './core/services/auth.service';
import { CatalogApiService } from './core/services/catalog-api.service';
import { CatalogMockService } from './core/services/catalog-mock.service';
import { CATALOG_SERVICE } from './core/services/catalog.service';
import { CommercialPartiesApiService } from './core/services/commercial-parties-api.service';
import { CommercialPartiesMockService } from './core/services/commercial-parties-mock.service';
import { COMMERCIAL_PARTIES_SERVICE } from './core/services/commercial-parties.service';
import { DirectoryMockService } from './core/services/directory-mock.service';
import { DIRECTORY_SERVICE } from './core/services/directory.service';
import { OrganizationApiService } from './core/services/organization-api.service';
import { OrganizationMockService } from './core/services/organization-mock.service';
import { ORGANIZATION_SERVICE } from './core/services/organization.service';
import { ProjectApiService } from './core/services/project-api.service';
import { ProjectMockService } from './core/services/project-mock.service';
import { PROJECT_SERVICE } from './core/services/project.service';

/**
 * The backend is the runtime path: session cookies (JSESSIONID) plus the
 * `XSRF-TOKEN`/`X-XSRF-TOKEN` CSRF flow through the same-origin dev proxy.
 * Set `APP_CONFIG.useMockApi` to `true` to run fully offline against fictional
 * fixtures (used by the unit-test harness).
 */
export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(
      routes,
      withInMemoryScrolling({ scrollPositionRestoration: 'top', anchorScrolling: 'enabled' }),
    ),
    provideHttpClient(
      withInterceptors([authErrorInterceptor]),
      withXsrfConfiguration({ cookieName: 'XSRF-TOKEN', headerName: 'X-XSRF-TOKEN' }),
    ),
    provideTranslateService({
      lang: 'fr',
      fallbackLang: 'en',
      loader: provideTranslateHttpLoader({ prefix: './assets/i18n/', suffix: '.json' }),
    }),
    {
      provide: PROJECT_SERVICE,
      useClass: APP_CONFIG.useMockApi ? ProjectMockService : ProjectApiService,
    },
    {
      // No directory endpoints exist in the frozen contract yet, so master
      // data is always served from fictional fixtures.
      provide: DIRECTORY_SERVICE,
      useClass: DirectoryMockService,
    },
    {
      provide: AUTH_SERVICE,
      useClass: APP_CONFIG.useMockApi ? AuthMockService : AuthApiService,
    },
    {
      provide: ACCOUNT_SERVICE,
      useClass: APP_CONFIG.useMockApi ? AccountMockService : AccountApiService,
    },
    {
      provide: ASSIGNMENT_SERVICE,
      useClass: APP_CONFIG.useMockApi ? AssignmentMockService : AssignmentApiService,
    },
    {
      provide: ORGANIZATION_SERVICE,
      useClass: APP_CONFIG.useMockApi ? OrganizationMockService : OrganizationApiService,
    },
    {
      provide: CATALOG_SERVICE,
      useClass: APP_CONFIG.useMockApi ? CatalogMockService : CatalogApiService,
    },
    {
      provide: COMMERCIAL_PARTIES_SERVICE,
      useClass: APP_CONFIG.useMockApi ? CommercialPartiesMockService : CommercialPartiesApiService,
    },
  ],
};
