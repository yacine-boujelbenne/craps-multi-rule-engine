import { provideHttpClient } from '@angular/common/http';
import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter, withInMemoryScrolling } from '@angular/router';
import { routes } from './app.routes';
import { APP_CONFIG } from './core/config/app-config';
import { DirectoryMockService } from './core/services/directory-mock.service';
import { DIRECTORY_SERVICE } from './core/services/directory.service';
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
      // The isolated mock keeps the UI fully functional until the backend is
      // reachable; flip `APP_CONFIG.useMockApi` to switch to HTTP.
      useClass: APP_CONFIG.useMockApi ? ProjectMockService : ProjectApiService,
    },
    {
      // No directory endpoints exist in the frozen contract yet, so master
      // data is always served from fictional fixtures.
      provide: DIRECTORY_SERVICE,
      useClass: DirectoryMockService,
    },
  ],
};
