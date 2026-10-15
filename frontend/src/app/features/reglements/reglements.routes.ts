import { Routes } from '@angular/router';

export default [
  { path: '', title: 'Payments', loadComponent: () => import('./reglement-list') },
  { path: ':id', title: 'Payment', loadComponent: () => import('./reglement-detail') },
] satisfies Routes;
