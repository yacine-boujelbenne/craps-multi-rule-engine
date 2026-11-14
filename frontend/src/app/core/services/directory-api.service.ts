import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { EMPTY, expand, map, reduce } from 'rxjs';
import { APP_CONFIG } from '../config/app-config';
import { Country, Organization, OrganizationRole, Port } from '../models/directory.model';
import { OrganizationPage } from '../models/commercial.model';
import { DirectoryService } from './directory.service';

@Injectable()
export class DirectoryApiService implements DirectoryService {
  private readonly http = inject(HttpClient);
  private readonly base = APP_CONFIG.apiBaseUrl;

  organizations(role?: OrganizationRole) {
    const page = (index: number) =>
      this.http.get<OrganizationPage>(`${this.base}/organizations`, {
        params: { page: index, size: 100, ...(role ? { role } : {}) },
      });
    return page(0).pipe(
      expand((result) => (result.page + 1 < result.totalPages ? page(result.page + 1) : EMPTY)),
      map((result) => result.items),
      reduce((all, items) => all.concat(items), [] as Organization[]),
    );
  }
  ports() {
    return this.http.get<Port[]>(`${this.base}/ports`);
  }
  countries() {
    return this.http.get<Country[]>(`${this.base}/countries`);
  }
}
