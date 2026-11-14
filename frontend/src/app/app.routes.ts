import { Routes } from '@angular/router';
import {
  adminGuard,
  authGuard,
  guestGuard,
  passwordChangeGuard,
  projectSetupGuard,
} from './core/guards/auth.guard';
import { AppShellComponent } from './layout/app-shell/app-shell.component';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
  {
    path: 'login',
    title: 'Sign in · ExportDocs',
    canActivate: [guestGuard],
    loadComponent: () =>
      import('./features/auth/login/login.component').then((m) => m.LoginComponent),
  },
  {
    path: 'change-password',
    title: 'Change password · ExportDocs',
    canActivate: [passwordChangeGuard],
    loadComponent: () =>
      import('./features/auth/change-password/change-password.component').then(
        (m) => m.ChangePasswordComponent,
      ),
  },
  {
    path: '',
    component: AppShellComponent,
    canActivate: [authGuard],
    children: [
      {
        path: 'dashboard',
        title: 'Dashboard · ExportDocs',
        loadComponent: () =>
          import('./features/dashboard/dashboard.component').then((m) => m.DashboardComponent),
      },
      {
        path: 'projects',
        title: 'Projects · ExportDocs',
        loadComponent: () =>
          import('./features/projects/project-list/project-list.component').then(
            (m) => m.ProjectListComponent,
          ),
      },
      {
        path: 'projects/new',
        title: 'New project · ExportDocs',
        canActivate: [adminGuard],
        loadComponent: () =>
          import('./features/projects/project-form/project-form.component').then(
            (m) => m.ProjectFormComponent,
          ),
      },
      {
        path: 'projects/:id/edit',
        title: 'Edit project · ExportDocs',
        canActivate: [projectSetupGuard],
        loadComponent: () =>
          import('./features/projects/project-form/project-form.component').then(
            (m) => m.ProjectFormComponent,
          ),
      },
      {
        path: 'projects/:id/access',
        title: 'Project access · ExportDocs',
        canActivate: [adminGuard],
        loadComponent: () =>
          import('./features/admin/project-access/project-access.component').then(
            (m) => m.ProjectAccessComponent,
          ),
      },
      {
        path: 'projects/:id',
        title: 'Project · ExportDocs',
        loadComponent: () =>
          import('./features/projects/project-details/project-details.component').then(
            (m) => m.ProjectDetailsComponent,
          ),
      },
      {
        path: 'admin/accounts',
        title: 'Accounts · ExportDocs',
        canActivate: [adminGuard],
        loadComponent: () =>
          import('./features/admin/accounts/account-list.component').then(
            (m) => m.AccountListComponent,
          ),
      },
      {
        path: 'partners',
        title: 'Companies · ExportDocs',
        canActivate: [adminGuard],
        loadComponent: () =>
          import('./features/partners/company-list/company-list.component').then(
            (m) => m.CompanyListComponent,
          ),
      },
      {
        path: 'catalog',
        title: 'Products · ExportDocs',
        canActivate: [adminGuard],
        loadComponent: () =>
          import('./features/catalog/product-list/product-list.component').then(
            (m) => m.ProductListComponent,
          ),
      },
    ],
  },
  {
    path: '**',
    title: 'Not found · ExportDocs',
    loadComponent: () =>
      import('./features/not-found/not-found.component').then((m) => m.NotFoundComponent),
  },
];
