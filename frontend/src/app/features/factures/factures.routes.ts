import { Routes } from '@angular/router';

export default [
  { path: '', title: 'Invoices', loadComponent: () => import('./facture-list') },
  { path: 'new', title: 'New invoice', loadComponent: () => import('./facture-form') },
  { path: ':id', title: 'Invoice', loadComponent: () => import('./facture-detail') },
] satisfies Routes;
