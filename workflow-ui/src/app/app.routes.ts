import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    redirectTo: 'templates',
    pathMatch: 'full'
  },
  {
    path: 'templates',
    loadComponent: () => import('./pages/dashboard/dashboard.component')
      .then(m => m.DashboardComponent),
    title: 'Workflow Templates'
  },
  {
    path: 'templates/:id',
    loadComponent: () => import('./pages/template-editor/template-editor.component')
      .then(m => m.TemplateEditorComponent),
    title: 'Edit Template'
  },
  {
    path: 'templates/new',
    loadComponent: () => import('./pages/template-editor/template-editor.component')
      .then(m => m.TemplateEditorComponent),
    title: 'New Template'
  },
  {
    path: '**',
    redirectTo: 'templates'
  }
];
