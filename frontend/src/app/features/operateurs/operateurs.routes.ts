import { Routes } from '@angular/router';

export default [
  { path: '', title: 'Operators', loadComponent: () => import('./operateur-list') },
  { path: 'new', title: 'New operator', loadComponent: () => import('./operateur-form') },
  { path: ':id', title: 'Operator', loadComponent: () => import('./operateur-detail') },
  { path: ':id/edit', title: 'Edit operator', loadComponent: () => import('./operateur-form') },
] satisfies Routes;
