import { Routes } from '@angular/router';

export default [
  { path: '', title: 'Products', loadComponent: () => import('./produit-list') },
  { path: 'new', title: 'New product', loadComponent: () => import('./produit-form') },
  { path: ':id', title: 'Product', loadComponent: () => import('./produit-detail') },
  { path: ':id/edit', title: 'Edit product', loadComponent: () => import('./produit-form') },
] satisfies Routes;
