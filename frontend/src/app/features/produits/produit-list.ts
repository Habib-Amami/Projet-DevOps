import { ChangeDetectionStrategy, Component, DestroyRef, inject } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { MatButton } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';
import { Router, RouterLink } from '@angular/router';
import { BehaviorSubject, forkJoin, map, switchMap } from 'rxjs';
import { loadState } from '../../core/api/load-state';
import { Produit } from '../../core/models/produit.model';
import { CategorieProduitService } from '../../core/services/categorie-produit.service';
import { ProduitService } from '../../core/services/produit.service';
import { StockService } from '../../core/services/stock.service';
import { AsyncState } from '../../shared/components/async-state';
import { DataTable, TableAction, TableColumn } from '../../shared/components/data-table';
import { EmptyState } from '../../shared/components/empty-state';
import { PageHeader } from '../../shared/components/page-header';
import { ConfirmService } from '../../shared/dialogs/confirm.service';

/** A product with its stock and category labels resolved for display. */
interface ProduitRow extends Produit {
  stockLabel: string | null;
  categorieLabel: string | null;
}

@Component({
  selector: 'app-produit-list',
  imports: [PageHeader, AsyncState, DataTable, EmptyState, MatButton, MatIcon, RouterLink],
  template: `
    <app-page-header title="Products">
      <a mat-flat-button routerLink="new"><mat-icon>add</mat-icon> New product</a>
    </app-page-header>
    <app-async-state [state]="state()" (retry)="reload()">
      @if (state().data; as produits) {
        @if (produits.length) {
          <app-data-table
            [rows]="produits"
            [columns]="columns"
            [actions]="actions"
            (rowClick)="open($event)"
          />
        } @else {
          <app-empty-state message="No products yet." />
        }
      }
    </app-async-state>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export default class ProduitList {
  private readonly api = inject(ProduitService);
  private readonly stockService = inject(StockService);
  private readonly categorieService = inject(CategorieProduitService);
  private readonly router = inject(Router);
  private readonly confirm = inject(ConfirmService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly reload$ = new BehaviorSubject<void>(undefined);

  protected readonly state = toSignal(
    this.reload$.pipe(
      switchMap(() =>
        loadState(
          forkJoin({
            produits: this.api.list(),
            stocks: this.stockService.list(),
            categories: this.categorieService.list(),
          }).pipe(
            map(({ produits, stocks, categories }) =>
              produits.map((p): ProduitRow => ({
                ...p,
                stockLabel: stocks.find((s) => s.idStock === p.stockId)?.libelleStock ?? null,
                categorieLabel:
                  categories.find((c) => c.idCategorieProduit === p.categorieProduitId)
                    ?.libelleCategorie ?? null,
              })),
            ),
          ),
        ),
      ),
    ),
    { requireSync: true },
  );

  protected readonly columns: TableColumn<ProduitRow>[] = [
    { key: 'id', header: 'ID', value: (p) => p.idProduit },
    { key: 'code', header: 'Code', value: (p) => p.codeProduit },
    { key: 'libelle', header: 'Label', value: (p) => p.libelleProduit },
    { key: 'prix', header: 'Price', value: (p) => p.prix, type: 'amount' },
    { key: 'stock', header: 'Stock', value: (p) => p.stockLabel },
    { key: 'categorie', header: 'Category', value: (p) => p.categorieLabel },
  ];

  protected readonly actions: TableAction<ProduitRow>[] = [
    {
      icon: 'edit',
      label: 'Edit',
      run: (p) => void this.router.navigate(['/produits', p.idProduit, 'edit']),
    },
    { icon: 'delete', label: 'Delete', warn: true, run: (p) => this.remove(p) },
  ];

  protected reload(): void {
    this.reload$.next();
  }

  protected open(produit: ProduitRow): void {
    void this.router.navigate(['/produits', produit.idProduit]);
  }

  private remove(produit: ProduitRow): void {
    this.confirm
      .confirmAndRun(
        { title: 'Delete product', message: `Delete product "${produit.libelleProduit}"?` },
        () => this.api.delete(produit.idProduit),
        'Product deleted',
      )
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.reload());
  }
}
