import { provideHttpClient } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { provideTranslateService } from '@ngx-translate/core';
import { firstValueFrom } from 'rxjs';
import { beforeEach, describe, expect, it } from 'vitest';
import { App } from './app';
import { routes } from './app.routes';
import { AssignmentMockService } from './core/services/assignment-mock.service';
import { ASSIGNMENT_SERVICE } from './core/services/assignment.service';
import { AuthMockService } from './core/services/auth-mock.service';
import { AUTH_SERVICE } from './core/services/auth.service';
import { AuthStore } from './core/services/auth.store';
import { DirectoryMockService } from './core/services/directory-mock.service';
import { DIRECTORY_SERVICE } from './core/services/directory.service';
import { MOCK_LATENCY_MS, ProjectMockService } from './core/services/project-mock.service';
import { PROJECT_SERVICE } from './core/services/project.service';

describe('App', () => {
  beforeEach(async () => {
    localStorage.clear();
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [
        provideRouter(routes),
        provideHttpClient(),
        provideTranslateService({ lang: 'fr', fallbackLang: 'en' }),
        { provide: MOCK_LATENCY_MS, useValue: 0 },
        { provide: PROJECT_SERVICE, useClass: ProjectMockService },
        { provide: DIRECTORY_SERVICE, useClass: DirectoryMockService },
        { provide: AUTH_SERVICE, useClass: AuthMockService },
        { provide: ASSIGNMENT_SERVICE, useClass: AssignmentMockService },
      ],
    }).compileComponents();

    await firstValueFrom(
      TestBed.inject(AuthStore).login({ username: 'admin', password: 'Admin-Passw0rd' }),
    );
  });

  it('creates the root component', () => {
    const fixture = TestBed.createComponent(App);
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('renders the application shell for a routed page', async () => {
    const fixture = TestBed.createComponent(App);
    await TestBed.inject(Router).navigateByUrl('/dashboard');
    await fixture.whenStable();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('app-shell')).toBeTruthy();
  });
});
