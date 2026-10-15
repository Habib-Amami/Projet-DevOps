import { Routes } from '@angular/router';

export default [
  { path: '', title: 'Suppliers', loadComponent: () => import('./fournisseur-list') },
  { path: 'new', title: 'New supplier', loadComponent: () => import('./fournisseur-form') },
  { path: ':id', title: 'Supplier', loadComponent: () => import('./fournisseur-detail') },
  { path: ':id/edit', title: 'Edit supplier', loadComponent: () => import('./fournisseur-form') },
] satisfies Routes;
