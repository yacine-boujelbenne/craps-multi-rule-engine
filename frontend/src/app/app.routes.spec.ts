import { provideHttpClient } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { beforeEach, describe, expect, it } from 'vitest';
import { App } from './app';
import { routes } from './app.routes';
import { DirectoryMockService } from './core/services/directory-mock.service';
import { DIRECTORY_SERVICE } from './core/services/directory.service';
import { MOCK_LATENCY_MS, ProjectMockService } from './core/services/project-mock.service';
import { PROJECT_SERVICE } from './core/services/project.service';

/**
 * Integration smoke test: boots the real app config (router + mock service),
 * exercises lazy-loaded routes and asserts the rendered output.
 */
describe('Application routes', () => {
  let fixture: ComponentFixture<App>;
  let router: Router;

  beforeEach(async () => {
    localStorage.clear();
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [
        provideRouter(routes),
        provideHttpClient(),
        { provide: MOCK_LATENCY_MS, useValue: 0 },
        { provide: PROJECT_SERVICE, useClass: ProjectMockService },
        { provide: DIRECTORY_SERVICE, useClass: DirectoryMockService },
      ],
    }).compileComponents();

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

  it('renders the project list with seeded projects', async () => {
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
    const el = await navigate('/projects/11111111-1111-4111-8111-000000000001');
    expect(el.textContent).toContain('Document workflow');
    expect(el.textContent).toContain('Invoice');
    expect(el.textContent).toContain('Certificate of Origin');
  });

  it('renders the 404 page for unknown routes', async () => {
    const el = await navigate('/does-not-exist');
    expect(el.textContent).toContain('Page not found');
  });
});
