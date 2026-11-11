import { provideHttpClient } from '@angular/common/http';
import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter, withInMemoryScrolling } from '@angular/router';
import { routes } from './app.routes';
import { APP_CONFIG } from './core/config/app-config';
import { ProjectApiService } from './core/services/project-api.service';
import { ProjectMockService } from './core/services/project-mock.service';
import { PROJECT_SERVICE } from './core/services/project.service';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(
      routes,
      withInMemoryScrolling({ scrollPositionRestoration: 'top', anchorScrolling: 'enabled' }),
    ),
    provideHttpClient(),
    {
      provide: PROJECT_SERVICE,
      // The isolated mock keeps the UI fully functional until the backend
      // OpenAPI contract is frozen; flip `APP_CONFIG.useMockApi` to switch.
      useClass: APP_CONFIG.useMockApi ? ProjectMockService : ProjectApiService,
    },
  ],
};
