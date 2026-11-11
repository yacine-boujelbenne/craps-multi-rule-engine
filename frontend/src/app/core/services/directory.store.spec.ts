import { TestBed } from '@angular/core/testing';
import { firstValueFrom, of } from 'rxjs';
import { beforeEach, describe, expect, it } from 'vitest';
import { Country, Organization, Port } from '../models/directory.model';
import { DIRECTORY_SERVICE } from './directory.service';
import { DirectoryStore } from './directory.store';

const ORGANIZATIONS: Organization[] = [
  { id: 'org-1', legalName: 'Exporter Co.', countryCode: 'MA', roles: ['EXPORTER'], active: true },
  {
    id: 'org-2',
    legalName: 'Maersk Line',
    countryCode: 'DK',
    roles: ['SHIPPING_LINE'],
    active: true,
  },
];

const PORTS: Port[] = [
  { id: 'port-1', name: 'Tanger Med', countryCode: 'MA', unLocode: 'MAPTM', active: true },
];

const COUNTRIES: Country[] = [{ code: 'MA', name: 'Morocco' }];

const directoryStub = {
  organizations: () => of(ORGANIZATIONS),
  ports: () => of(PORTS),
  countries: () => of(COUNTRIES),
};

describe('DirectoryStore', () => {
  let store: DirectoryStore;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [{ provide: DIRECTORY_SERVICE, useValue: directoryStub }],
    });
    store = TestBed.inject(DirectoryStore);
    store.ensureLoaded();
  });

  it('loads master data once', () => {
    expect(store.loaded()).toBe(true);
    expect(store.organizations().length).toBe(2);
    expect(store.ports().length).toBe(1);
    expect(store.countries().length).toBe(1);
  });

  it('resolves organization and port names by id', () => {
    expect(store.organizationName('org-1')).toBe('Exporter Co.');
    expect(store.organizationName('unknown')).toBe('—');
    expect(store.portName('port-1')).toBe('Tanger Med');
    expect(store.countryName('MA')).toBe('Morocco');
    expect(store.countryName('ZZ')).toBe('ZZ');
  });

  it('filters organizations by role', () => {
    const lines = store.organizationsByRole('SHIPPING_LINE');
    expect(lines).toHaveLength(1);
    expect(lines[0].legalName).toBe('Maersk Line');
  });

  it('exposes the underlying directory streams', async () => {
    const service = TestBed.inject(DIRECTORY_SERVICE);
    const ports = await firstValueFrom(service.ports());
    expect(ports).toHaveLength(1);
  });
});
