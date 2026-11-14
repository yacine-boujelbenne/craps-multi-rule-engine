import { provideHttpClient, withInterceptors, withXsrfConfiguration } from '@angular/common/http';
import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter, withInMemoryScrolling } from '@angular/router';
import { provideTranslateService } from '@ngx-translate/core';
import { provideTranslateHttpLoader } from '@ngx-translate/http-loader';
import { routes } from './app.routes';
import { authErrorInterceptor } from './core/interceptors/auth-error.interceptor';
import { AccountApiService } from './core/services/account-api.service';
import { ACCOUNT_SERVICE } from './core/services/account.service';
import { AssignmentApiService } from './core/services/assignment-api.service';
import { ASSIGNMENT_SERVICE } from './core/services/assignment.service';
import { AuthApiService } from './core/services/auth-api.service';
import { AUTH_SERVICE } from './core/services/auth.service';
import { CatalogApiService } from './core/services/catalog-api.service';
import { CATALOG_SERVICE } from './core/services/catalog.service';
import { CommercialPartiesApiService } from './core/services/commercial-parties-api.service';
import { COMMERCIAL_PARTIES_SERVICE } from './core/services/commercial-parties.service';
import { CompanyAddressApiService } from './core/services/company-address-api.service';
import { COMPANY_ADDRESS_SERVICE } from './core/services/company-address.service';
import { DirectoryApiService } from './core/services/directory-api.service';
import { DIRECTORY_SERVICE } from './core/services/directory.service';
import { GeographyApiService } from './core/services/geography-api.service';
import { GEOGRAPHY_SERVICE } from './core/services/geography.service';
import { OrganizationApiService } from './core/services/organization-api.service';
import { ORGANIZATION_SERVICE } from './core/services/organization.service';
import { ProjectApiService } from './core/services/project-api.service';
import { ShipmentProductApiService } from './core/services/shipment-product-api.service';
import { SHIPMENT_PRODUCT_SERVICE } from './core/services/shipment-product.service';
import { TaxonomyApiService } from './core/services/taxonomy-api.service';
import { TAXONOMY_SERVICE } from './core/services/taxonomy.service';
import { PROJECT_SERVICE } from './core/services/project.service';

/**
 * The backend is the runtime path: session cookies (JSESSIONID) plus the
 * `XSRF-TOKEN`/`X-XSRF-TOKEN` CSRF flow through the same-origin dev proxy.
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
      useClass: ProjectApiService,
    },
    {
      provide: DIRECTORY_SERVICE,
      useClass: DirectoryApiService,
    },
    {
      provide: AUTH_SERVICE,
      useClass: AuthApiService,
    },
    {
      provide: ACCOUNT_SERVICE,
      useClass: AccountApiService,
    },
    {
      provide: ASSIGNMENT_SERVICE,
      useClass: AssignmentApiService,
    },
    {
      provide: GEOGRAPHY_SERVICE,
      useClass: GeographyApiService,
    },
    {
      provide: COMPANY_ADDRESS_SERVICE,
      useClass: CompanyAddressApiService,
    },
    {
      provide: SHIPMENT_PRODUCT_SERVICE,
      useClass: ShipmentProductApiService,
    },
    {
      provide: TAXONOMY_SERVICE,
      useClass: TaxonomyApiService,
    },
    {
      provide: ORGANIZATION_SERVICE,
      useClass: OrganizationApiService,
    },
    {
      provide: CATALOG_SERVICE,
      useClass: CatalogApiService,
    },
    {
      provide: COMMERCIAL_PARTIES_SERVICE,
      useClass: CommercialPartiesApiService,
    },
  ],
};
