import { Routes } from '@angular/router';

export default [
  { path: '', title: 'Business sectors', loadComponent: () => import('./secteur-list') },
  { path: 'new', title: 'New sector', loadComponent: () => import('./secteur-form') },
  { path: ':id', title: 'Business sector', loadComponent: () => import('./secteur-detail') },
  { path: ':id/edit', title: 'Edit sector', loadComponent: () => import('./secteur-form') },
] satisfies Routes;
