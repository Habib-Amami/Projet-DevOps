import { DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, inject, input } from '@angular/core';
import { takeUntilDestroyed, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { MatButton } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';
import { Router, RouterLink } from '@angular/router';
import { BehaviorSubject, combineLatest, forkJoin, map, switchMap } from 'rxjs';
import { LOADING, loadState } from '../../core/api/load-state';
import { Produit } from '../../core/models/produit.model';
import { CategorieProduitService } from '../../core/services/categorie-produit.service';
import { ProduitService } from '../../core/services/produit.service';
import { StockService } from '../../core/services/stock.service';
import { AsyncState } from '../../shared/components/async-state';
import { PageHeader } from '../../shared/components/page-header';
import { ConfirmService } from '../../shared/dialogs/confirm.service';

@Component({
  selector: 'app-produit-detail',
  imports: [PageHeader, AsyncState, MatButton, MatIcon, RouterLink, DecimalPipe],
  template: `
    <app-async-state [state]="state()" (retry)="reload()">
      @if (state().data; as view) {
        <app-page-header [title]="view.produit.libelleProduit" backLink="/produits">
          <a mat-stroked-button routerLink="edit"><mat-icon>edit</mat-icon> Edit</a>
          <button mat-flat-button class="danger" type="button" (click)="remove(view.produit)">
            <mat-icon>delete</mat-icon> Delete
          </button>
        </app-page-header>
        <dl class="details">
          <dt>ID</dt>
          <dd>{{ view.produit.idProduit }}</dd>
          <dt>Code</dt>
          <dd>{{ view.produit.codeProduit }}</dd>
          <dt>Label</dt>
          <dd>{{ view.produit.libelleProduit }}</dd>
          <dt>Price</dt>
          <dd>{{ view.produit.prix | number: '1.2-2' }}</dd>
          <dt>Stock</dt>
          <dd>
            @if (view.produit.stockId; as stockId) {
              <a [routerLink]="['/stocks', stockId]">{{ view.stockLabel ?? '#' + stockId }}</a>
            } @else {
              —
            }
          </dd>
          <dt>Category</dt>
          <dd>
            @if (view.produit.categorieProduitId; as categorieId) {
              <a [routerLink]="['/categories-produits', categorieId]">
                {{ view.categorieLabel ?? '#' + categorieId }}
              </a>
            } @else {
              —
            }
          </dd>
        </dl>
      }
    </app-async-state>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export default class ProduitDetail {
  private readonly api = inject(ProduitService);
  private readonly stockService = inject(StockService);
  private readonly categorieService = inject(CategorieProduitService);
  private readonly router = inject(Router);
  private readonly confirm = inject(ConfirmService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly reload$ = new BehaviorSubject<void>(undefined);

  readonly id = input.required<string>();

  protected readonly state = toSignal(
    combineLatest([toObservable(this.id), this.reload$]).pipe(
      switchMap(([id]) =>
        loadState(
          forkJoin({
            produit: this.api.get(Number(id)),
            stocks: this.stockService.list(),
            categories: this.categorieService.list(),
          }).pipe(
            map(({ produit, stocks, categories }) => ({
              produit,
              stockLabel: stocks.find((s) => s.idStock === produit.stockId)?.libelleStock ?? null,
              categorieLabel:
                categories.find((c) => c.idCategorieProduit === produit.categorieProduitId)
                  ?.libelleCategorie ?? null,
            })),
          ),
        ),
      ),
    ),
    { initialValue: LOADING },
  );

  protected reload(): void {
    this.reload$.next();
  }

  protected remove(produit: Produit): void {
    this.confirm
      .confirmAndRun(
        { title: 'Delete product', message: `Delete product "${produit.libelleProduit}"?` },
        () => this.api.delete(produit.idProduit),
        'Product deleted',
      )
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => void this.router.navigate(['/produits']));
  }
}
