import { Routes } from '@angular/router';

export default [
  { path: '', title: 'Stocks', loadComponent: () => import('./stock-list') },
  { path: 'new', title: 'New stock', loadComponent: () => import('./stock-form') },
  { path: ':id', title: 'Stock', loadComponent: () => import('./stock-detail') },
  { path: ':id/edit', title: 'Edit stock', loadComponent: () => import('./stock-form') },
] satisfies Routes;
