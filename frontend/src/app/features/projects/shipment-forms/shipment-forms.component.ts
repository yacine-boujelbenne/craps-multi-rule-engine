import { Component, OnInit, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { toApiProblem } from '../../../core/utils/problem';
import { components } from '../../../core/api/master-data-schema';
import { ORGANIZATION_SERVICE } from '../../../core/services/organization.service';
import { COMPANY_ADDRESS_SERVICE } from '../../../core/services/company-address.service';
import { APP_CONFIG } from '../../../core/config/app-config';
import { PROJECT_SERVICE } from '../../../core/services/project.service';
import { SHIPMENT_PRODUCT_SERVICE } from '../../../core/services/shipment-product.service';
import { ASSIGNMENT_SERVICE } from '../../../core/services/assignment.service';
import { DIRECTORY_SERVICE } from '../../../core/services/directory.service';
import { CATALOG_SERVICE } from '../../../core/services/catalog.service';
import { COMMERCIAL_PARTIES_SERVICE } from '../../../core/services/commercial-parties.service';
import { AuthStore } from '../../../core/services/auth.store';
import { NotificationService } from '../../../core/services/notification.service';
import { Project, isApiProblemError } from '../../../core/models/project.model';
import { ProjectProduct } from '../../../core/models/master-data.model';

type FormData = components['schemas']['ShipmentForms'];

@Component({
  selector: 'app-shipment-forms',
  imports: [FormsModule, RouterLink],
  templateUrl: './shipment-forms.component.html',
  styleUrl: './shipment-forms.component.scss',
})
export class ShipmentFormsComponent implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly route = inject(ActivatedRoute);
  private readonly projects = inject(PROJECT_SERVICE);
  private readonly products = inject(SHIPMENT_PRODUCT_SERVICE);
  private readonly assignments = inject(ASSIGNMENT_SERVICE);
  private readonly directory = inject(DIRECTORY_SERVICE);
  private readonly catalog = inject(CATALOG_SERVICE);
  private readonly organizations = inject(ORGANIZATION_SERVICE);
  private readonly addresses = inject(COMPANY_ADDRESS_SERVICE);
  private readonly parties = inject(COMMERCIAL_PARTIES_SERVICE);
  private readonly auth = inject(AuthStore);
  private readonly notifications = inject(NotificationService);
  readonly id = this.route.snapshot.paramMap.get('id')!;
  readonly base = `${APP_CONFIG.apiBaseUrl}/projects/${this.id}/forms`;
  readonly loading = signal(true);
  readonly error = signal('');
  readonly saving = signal(false);
  readonly dirty = signal(false);
  private readonly unsaved = new Set<string>();
  readonly countries = signal<{ code: string; name: string }[]>([]);
  readonly companyDetails = signal<{ name: string; details: string[] }[]>([]);
  readonly pendingDetails = signal(0);
  mark(key: string) {
    this.unsaved.add(key);
    this.dirty.set(true);
  }
  readonly project = signal<Project | null>(null);
  readonly forms = signal<FormData | null>(null);
  readonly lines = signal<ProjectProduct[]>([]);
  readonly summary = signal<{ label: string; value: string }[]>([]);
  readonly productNames = signal<Record<string, string>>({});
  editable: string[] = [];
  ngOnInit() {
    this.load();
  }
  load() {
    if (this.dirty() && !window.confirm('Discard unsaved changes and reload?')) return;
    this.loading.set(true);
    this.error.set('');
    this.companyDetails.set([]);
    forkJoin({
      project: this.projects.get(this.id),
      forms: this.http.get<FormData>(this.base),
      products: this.products.listProducts(this.id),
      permissions: this.assignments.permissions(this.id),
      companies: this.directory.organizations(),
      countries: this.directory.countries(),
      ports: this.directory.ports(),
      parties: this.parties.get(this.id),
    }).subscribe({
      next: (r) => {
        if (
          r.project.version !== r.forms.projectVersion ||
          r.parties.version !== r.forms.projectVersion ||
          r.products.some((p) => p.projectVersion !== r.forms.projectVersion)
        ) {
          this.error.set('Shipment changed while loading. Reload before editing or printing.');
          this.loading.set(false);
          return;
        }
        this.countries.set(r.countries);
        const companyIds = new Set(
          [
            r.project.exporterId,
            ...r.project.producerIds,
            r.project.consigneeId,
            r.project.notifyPartyId,
            r.project.freightForwarderId,
            r.project.shippingLineId,
            r.parties.clientId,
            r.parties.importerId,
            r.parties.supplierId,
          ].filter((id): id is string => !!id),
        );
        this.pendingDetails.set(companyIds.size + new Set(r.products.map((p) => p.productId)).size);
        for (const id of companyIds)
          forkJoin({
            company: this.organizations.get(id),
            addresses: this.addresses.list(id),
          }).subscribe({
            next: ({ company, addresses }) => {
              this.companyDetails.update((all) => [
                ...all,
                {
                  name: company.legalName,
                  details: [
                    company.tradeName,
                    company.countryCode,
                    company.registrationNumber,
                    company.taxIdentifier,
                    company.email,
                    company.phone,
                    company.addressText,
                    ...addresses.map((a) =>
                      [
                        a.addressLine1,
                        a.addressLine2,
                        a.manualCity ?? a.cityId,
                        a.postalCode,
                        a.countryCode,
                      ]
                        .filter(Boolean)
                        .join(', '),
                    ),
                  ].filter((v): v is string => !!v),
                },
              ]);
              this.pendingDetails.update((n) => n - 1);
            },
            error: () => {
              this.error.set('Unable to load company details.');
              this.pendingDetails.update((n) => n - 1);
            },
          });
        this.project.set(r.project);
        this.forms.set(this.prepare(r.forms));
        this.lines.set(r.products);
        this.editable = r.permissions.editableForms;
        const company = (id: string | null) =>
          r.companies.find((c) => c.id === id)?.legalName ?? id ?? '—';
        const port = (id: string | null) => r.ports.find((p) => p.id === id)?.name ?? id ?? '—';
        this.summary.set([
          { label: 'Reference', value: r.project.reference },
          { label: 'Exporter', value: company(r.project.exporterId) },
          { label: 'Producer', value: r.project.producerIds.map(company).join(', ') },
          { label: 'Consignee', value: company(r.project.consigneeId) },
          { label: 'Client', value: company(r.parties.clientId) },
          { label: 'Importer', value: company(r.parties.importerId) },
          { label: 'Supplier', value: company(r.parties.supplierId) },
          { label: 'Notify party', value: company(r.project.notifyPartyId) },
          { label: 'Forwarder', value: company(r.project.freightForwarderId) },
          { label: 'Carrier', value: company(r.project.shippingLineId) },
          { label: 'Destination country', value: r.project.destinationCountryCode ?? '—' },
          { label: 'Loading port', value: port(r.project.portOfLoadingId) },
          { label: 'Discharge port', value: port(r.project.portOfDischargeId) },
          { label: 'ETD', value: r.project.expectedDepartureDate ?? '—' },
          { label: 'Notes', value: r.project.notes ?? '—' },
        ]);
        for (const id of new Set(r.products.map((p) => p.productId)))
          this.catalog.getProduct(id).subscribe({
            next: (p) => {
              this.productNames.update((names) => ({
                ...names,
                [id]: [
                  p.name,
                  p.scientificName,
                  p.hsCode,
                  p.description,
                  p.processingMethod,
                  p.storageRequirements,
                ]
                  .filter(Boolean)
                  .join(' · '),
              }));
              this.pendingDetails.update((n) => n - 1);
            },
            error: () => {
              this.error.set('A product name could not be loaded. Reload before printing.');
              this.pendingDetails.update((n) => n - 1);
            },
          });
        this.loading.set(false);
        this.unsaved.clear();
        this.dirty.set(false);
      },
      error: () => {
        this.error.set('Unable to load shipment data.');
        this.loading.set(false);
      },
    });
  }
  private prepare(forms: FormData) {
    for (const section of forms.sections) forms.data[section.key] ??= [{}];
    return forms;
  }
  canEdit(key: string) {
    return this.auth.isAdmin() || this.editable.includes(key);
  }
  add(key: string) {
    this.forms()!.data[key].push({});
    this.mark(key);
  }
  remove(key: string, index: number) {
    this.forms()!.data[key].splice(index, 1);
    this.mark(key);
  }
  save(key: string) {
    const forms = this.forms()!;
    this.saving.set(true);
    this.http
      .put<FormData>(`${this.base}/${key}`, {
        expectedProjectVersion: forms.projectVersion,
        data: forms.data[key],
      })
      .pipe(catchError(toApiProblem))
      .subscribe({
        next: (saved) => {
          // Preserve unsaved edits in other sections when the shared revision advances.
          forms.projectVersion = saved.projectVersion;
          this.unsaved.delete(key);
          this.dirty.set(this.unsaved.size > 0);
          this.saving.set(false);
          this.notifications.success('Section saved.');
        },
        error: (error) => {
          this.saving.set(false);
          this.notifications.error(
            isApiProblemError(error)
              ? (error.problem.detail ?? 'Unable to save.')
              : 'Unable to save. Reload if the shipment changed.',
          );
        },
      });
  }
  print() {
    window.print();
  }
}
