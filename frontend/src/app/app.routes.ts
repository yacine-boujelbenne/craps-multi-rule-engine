import { Routes } from '@angular/router';
import { AppShellComponent } from './layout/app-shell/app-shell.component';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
  {
    path: '',
    component: AppShellComponent,
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
        loadComponent: () =>
          import('./features/projects/project-form/project-form.component').then(
            (m) => m.ProjectFormComponent,
          ),
      },
      {
        path: 'projects/:id/edit',
        title: 'Edit project · ExportDocs',
        loadComponent: () =>
          import('./features/projects/project-form/project-form.component').then(
            (m) => m.ProjectFormComponent,
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
    ],
  },
  {
    path: '**',
    title: 'Not found · ExportDocs',
    loadComponent: () =>
      import('./features/not-found/not-found.component').then((m) => m.NotFoundComponent),
  },
];
