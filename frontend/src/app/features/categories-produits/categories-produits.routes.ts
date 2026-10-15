import { Routes } from '@angular/router';

export default [
  { path: '', title: 'Product categories', loadComponent: () => import('./categorie-list') },
  { path: 'new', title: 'New category', loadComponent: () => import('./categorie-form') },
  { path: ':id', title: 'Product category', loadComponent: () => import('./categorie-detail') },
  { path: ':id/edit', title: 'Edit category', loadComponent: () => import('./categorie-form') },
] satisfies Routes;
