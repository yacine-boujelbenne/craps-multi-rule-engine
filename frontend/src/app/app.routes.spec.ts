import { provideHttpClient } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
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
import { CommercialPartiesMockService } from './core/services/commercial-parties-mock.service';
import { COMMERCIAL_PARTIES_SERVICE } from './core/services/commercial-parties.service';
import { DirectoryMockService } from './core/services/directory-mock.service';
import { DIRECTORY_SERVICE } from './core/services/directory.service';
import { OrganizationMockService } from './core/services/organization-mock.service';
import { ORGANIZATION_SERVICE } from './core/services/organization.service';
import { MOCK_LATENCY_MS, ProjectMockService } from './core/services/project-mock.service';
import { PROJECT_SERVICE } from './core/services/project.service';

/**
 * Integration smoke test: boots the real app config (router + mock service),
 * exercises lazy-loaded routes and asserts the rendered output.
 */
describe('Application routes', () => {
  let fixture: ComponentFixture<App>;
  let router: Router;
  let projectId: string;

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
        { provide: ORGANIZATION_SERVICE, useClass: OrganizationMockService },
        { provide: COMMERCIAL_PARTIES_SERVICE, useClass: CommercialPartiesMockService },
      ],
    }).compileComponents();

    await firstValueFrom(
      TestBed.inject(AuthStore).login({ username: 'admin', password: 'Admin-Passw0rd' }),
    );
    const created = await firstValueFrom(
      TestBed.inject(PROJECT_SERVICE).create({ reference: 'EXP-2026-0001', producerIds: [] }),
    );
    projectId = created.id;

    fixture = TestBed.createComponent(App);
    router = TestBed.inject(Router);
  });

  async function navigate(url: string): Promise<HTMLElement> {
    await router.navigateByUrl(url);
    // Allow the mock service latency to resolve and signals to propagate.
    await new Promise((resolve) => setTimeout(resolve, 450));
    await fixture.whenStable();
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  it('renders the dashboard with key figures', async () => {
    const el = await navigate('/dashboard');
    expect(el.querySelector('app-shell')).toBeTruthy();
    expect(el.textContent).toContain('Total projects');
  });

  it('renders the project list with the created project', async () => {
    const el = await navigate('/projects');
    expect(el.querySelector('app-project-list')).toBeTruthy();
    expect(el.textContent).toContain('EXP-2026-');
  });

  it('renders the create-project form', async () => {
    const el = await navigate('/projects/new');
    expect(el.querySelector('app-project-form')).toBeTruthy();
    expect(el.textContent).toContain('Business partners');
  });

  it('renders a project workspace with the document workflow', async () => {
    const el = await navigate(`/projects/${projectId}`);
    expect(el.textContent).toContain('Document workflow');
    expect(el.textContent).toContain('Invoice');
    expect(el.textContent).toContain('Certificate of Origin');
  });

  it('renders the 404 page for unknown routes', async () => {
    const el = await navigate('/does-not-exist');
    expect(el.textContent).toContain('Page not found');
  });
});
