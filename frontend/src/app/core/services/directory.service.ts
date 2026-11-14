import { InjectionToken } from '@angular/core';
import { Observable } from 'rxjs';
import { Country, Organization, OrganizationRole, Port } from '../models/directory.model';

export interface DirectoryService {
  organizations(role?: OrganizationRole): Observable<Organization[]>;
  ports(): Observable<Port[]>;
  countries(): Observable<Country[]>;
}

export const DIRECTORY_SERVICE = new InjectionToken<DirectoryService>('DIRECTORY_SERVICE');
