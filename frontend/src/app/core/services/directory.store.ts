import { Injectable, computed, inject, signal } from '@angular/core';
import { forkJoin } from 'rxjs';
import { Country, Organization, OrganizationRole, Port } from '../models/directory.model';
import { DIRECTORY_SERVICE } from './directory.service';

/**
 * Loads master-data once and exposes lookup helpers so components can render
 * organization/port/country names from the ids stored on a project.
 */
@Injectable({ providedIn: 'root' })
export class DirectoryStore {
  private readonly service = inject(DIRECTORY_SERVICE);

  private readonly _organizations = signal<Organization[]>([]);
  private readonly _ports = signal<Port[]>([]);
  private readonly _countries = signal<Country[]>([]);
  private readonly _loading = signal(false);
  private readonly _loaded = signal(false);

  readonly organizations = this._organizations.asReadonly();
  readonly ports = this._ports.asReadonly();
  readonly countries = this._countries.asReadonly();
  readonly loading = this._loading.asReadonly();
  readonly loaded = this._loaded.asReadonly();

  private readonly organizationMap = computed(
    () => new Map(this._organizations().map((organization) => [organization.id, organization])),
  );
  private readonly portMap = computed(() => new Map(this._ports().map((port) => [port.id, port])));
  private readonly countryMap = computed(
    () => new Map(this._countries().map((country) => [country.code, country])),
  );

  ensureLoaded(): void {
    if (this._loaded() || this._loading()) {
      return;
    }
    this._loading.set(true);
    forkJoin({
      organizations: this.service.organizations(),
      ports: this.service.ports(),
      countries: this.service.countries(),
    }).subscribe({
      next: ({ organizations, ports, countries }) => {
        this._organizations.set(organizations);
        this._ports.set(ports);
        this._countries.set(countries);
        this._loaded.set(true);
        this._loading.set(false);
      },
      error: () => {
        this._loading.set(false);
      },
    });
  }

  organization(id: string | null | undefined): Organization | undefined {
    return id ? this.organizationMap().get(id) : undefined;
  }

  organizationName(id: string | null | undefined): string {
    return this.organization(id)?.legalName ?? '—';
  }

  organizationNames(ids: string[] | null | undefined): string[] {
    return (ids ?? []).map((id) => this.organizationName(id));
  }

  portName(id: string | null | undefined): string {
    return (id ? this.portMap().get(id)?.name : undefined) ?? '—';
  }

  countryName(code: string | null | undefined): string {
    if (!code) {
      return '—';
    }
    return this.countryMap().get(code)?.name ?? code;
  }

  organizationsByRole(role: OrganizationRole): Organization[] {
    return this._organizations().filter((organization) => organization.roles.includes(role));
  }
}
