import { Routes } from '@angular/router';
import { Shell } from './core/layout/shell';

export const routes: Routes = [
  {
    path: '',
    component: Shell,
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'factures' },
      {
        path: 'factures',
        loadChildren: () => import('./features/factures/factures.routes'),
      },
      {
        path: 'reglements',
        loadChildren: () => import('./features/reglements/reglements.routes'),
      },
      {
        path: 'produits',
        loadChildren: () => import('./features/produits/produits.routes'),
      },
      {
        path: 'categories-produits',
        loadChildren: () => import('./features/categories-produits/categories-produits.routes'),
      },
      {
        path: 'stocks',
        loadChildren: () => import('./features/stocks/stocks.routes'),
      },
      {
        path: 'fournisseurs',
        loadChildren: () => import('./features/fournisseurs/fournisseurs.routes'),
      },
      {
        path: 'secteurs-activite',
        loadChildren: () => import('./features/secteurs-activite/secteurs-activite.routes'),
      },
      {
        path: 'operateurs',
        loadChildren: () => import('./features/operateurs/operateurs.routes'),
      },
      {
        path: '**',
        title: 'Page not found',
        loadComponent: () => import('./features/not-found/not-found'),
      },
    ],
  },
];
