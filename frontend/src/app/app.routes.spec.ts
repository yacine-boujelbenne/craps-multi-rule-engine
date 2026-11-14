import { HttpClient, provideHttpClient } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { provideTranslateService } from '@ngx-translate/core';
import { firstValueFrom, of } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { App } from './app';
import { routes } from './app.routes';
import { AssignmentMockService } from './core/services/assignment-mock.service';
import { ASSIGNMENT_SERVICE } from './core/services/assignment.service';
import { AuthMockService } from './core/services/auth-mock.service';
import { AUTH_SERVICE } from './core/services/auth.service';
import { AuthStore } from './core/services/auth.store';
import { CatalogMockService } from './core/services/catalog-mock.service';
import { CATALOG_SERVICE } from './core/services/catalog.service';
import { CommercialPartiesMockService } from './core/services/commercial-parties-mock.service';
import { COMMERCIAL_PARTIES_SERVICE } from './core/services/commercial-parties.service';
import { CompanyAddressMockService } from './core/services/company-address-mock.service';
import { COMPANY_ADDRESS_SERVICE } from './core/services/company-address.service';
import { DirectoryMockService } from './core/services/directory-mock.service';
import { DIRECTORY_SERVICE } from './core/services/directory.service';
import { GeographyMockService } from './core/services/geography-mock.service';
import { GEOGRAPHY_SERVICE } from './core/services/geography.service';
import { OrganizationMockService } from './core/services/organization-mock.service';
import { ORGANIZATION_SERVICE } from './core/services/organization.service';
import { MOCK_LATENCY_MS, ProjectMockService } from './core/services/project-mock.service';
import { PROJECT_SERVICE } from './core/services/project.service';
import { ShipmentProductMockService } from './core/services/shipment-product-mock.service';
import { SHIPMENT_PRODUCT_SERVICE } from './core/services/shipment-product.service';
import { TaxonomyMockService } from './core/services/taxonomy-mock.service';
import { TAXONOMY_SERVICE } from './core/services/taxonomy.service';

/**
 * Integration smoke test: boots the real app config (router + mock service),
 * exercises lazy-loaded routes and asserts the rendered output.
 */
describe('Application routes', () => {
  let fixture: ComponentFixture<App>;
  let router: Router;
  let projectId: string;
  const formResponse = () => ({
    projectVersion: 0,
    sections: [
      {
        key: 'INVOICE',
        title: 'Invoice',
        repeatable: false,
        fields: [{ key: 'number', label: 'Invoice number', type: 'text' }],
      },
    ],
    data: {},
  });
  const http = {
    get: vi.fn(() => of(formResponse())),
    put: vi.fn(() => of({ ...formResponse(), projectVersion: 1 })),
  };

  beforeEach(async () => {
    localStorage.clear();
    http.get.mockClear();
    http.put.mockClear();
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [
        provideRouter(routes),
        provideHttpClient(),
        { provide: HttpClient, useValue: http },
        provideTranslateService({ lang: 'fr', fallbackLang: 'en' }),
        { provide: MOCK_LATENCY_MS, useValue: 0 },
        { provide: PROJECT_SERVICE, useClass: ProjectMockService },
        { provide: DIRECTORY_SERVICE, useClass: DirectoryMockService },
        { provide: AUTH_SERVICE, useClass: AuthMockService },
        { provide: ASSIGNMENT_SERVICE, useClass: AssignmentMockService },
        { provide: ORGANIZATION_SERVICE, useClass: OrganizationMockService },
        { provide: COMMERCIAL_PARTIES_SERVICE, useClass: CommercialPartiesMockService },
        { provide: CATALOG_SERVICE, useClass: CatalogMockService },
        { provide: GEOGRAPHY_SERVICE, useClass: GeographyMockService },
        { provide: COMPANY_ADDRESS_SERVICE, useClass: CompanyAddressMockService },
        { provide: SHIPMENT_PRODUCT_SERVICE, useClass: ShipmentProductMockService },
        { provide: TAXONOMY_SERVICE, useClass: TaxonomyMockService },
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
    expect(el.textContent).toContain('Document forms and printable summary');
  });

  it('loads document fields, saves an edited section and enables printing', async () => {
    const el = await navigate(`/projects/${projectId}/forms`);
    expect(el.textContent).toContain('Internal shipment summary');
    const input = el.querySelector('app-shipment-forms input') as HTMLInputElement;
    input.value = 'INV-REAL';
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    const print = Array.from(el.querySelectorAll('button')).find((b) =>
      b.textContent?.includes('Print /'),
    )!;
    expect(print.disabled).toBe(true);
    Array.from(el.querySelectorAll('button'))
      .find((b) => b.textContent?.includes('Save Invoice'))!
      .click();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(http.put).toHaveBeenCalledWith(`/api/projects/${projectId}/forms/INVOICE`, {
      expectedProjectVersion: 0,
      data: [{ number: 'INV-REAL' }],
    });
    expect(print.disabled).toBe(false);
  });

  it('renders the 404 page for unknown routes', async () => {
    const el = await navigate('/does-not-exist');
    expect(el.textContent).toContain('Page not found');
  });
});
