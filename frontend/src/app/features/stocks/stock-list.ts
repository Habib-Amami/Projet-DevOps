import { ChangeDetectionStrategy, Component, DestroyRef, inject } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { MatButton } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';
import { Router, RouterLink } from '@angular/router';
import { BehaviorSubject, switchMap } from 'rxjs';
import { loadState } from '../../core/api/load-state';
import { Stock } from '../../core/models/stock.model';
import { StockService } from '../../core/services/stock.service';
import { AsyncState } from '../../shared/components/async-state';
import { DataTable, TableAction, TableColumn } from '../../shared/components/data-table';
import { EmptyState } from '../../shared/components/empty-state';
import { PageHeader } from '../../shared/components/page-header';
import { ConfirmService } from '../../shared/dialogs/confirm.service';

@Component({
  selector: 'app-stock-list',
  imports: [PageHeader, AsyncState, DataTable, EmptyState, MatButton, MatIcon, RouterLink],
  template: `
    <app-page-header title="Stocks">
      <a mat-flat-button routerLink="new"><mat-icon>add</mat-icon> New stock</a>
    </app-page-header>
    <app-async-state [state]="state()" (retry)="reload()">
      @if (state().data; as stocks) {
        @if (stocks.length) {
          <app-data-table
            [rows]="stocks"
            [columns]="columns"
            [actions]="actions"
            (rowClick)="open($event)"
          />
        } @else {
          <app-empty-state message="No stocks yet." />
        }
      }
    </app-async-state>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export default class StockList {
  private readonly api = inject(StockService);
  private readonly router = inject(Router);
  private readonly confirm = inject(ConfirmService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly reload$ = new BehaviorSubject<void>(undefined);

  protected readonly state = toSignal(
    this.reload$.pipe(switchMap(() => loadState(this.api.list()))),
    { requireSync: true },
  );

  protected readonly columns: TableColumn<Stock>[] = [
    { key: 'id', header: 'ID', value: (s) => s.idStock },
    { key: 'libelle', header: 'Label', value: (s) => s.libelleStock },
    { key: 'qte', header: 'Quantity', value: (s) => s.qte },
    { key: 'qteMin', header: 'Min. quantity', value: (s) => s.qteMin },
    { key: 'low', header: 'Below minimum', value: (s) => s.qte < s.qteMin, type: 'boolean' },
  ];

  protected readonly actions: TableAction<Stock>[] = [
    {
      icon: 'edit',
      label: 'Edit',
      run: (s) => void this.router.navigate(['/stocks', s.idStock, 'edit']),
    },
    { icon: 'delete', label: 'Delete', warn: true, run: (s) => this.remove(s) },
  ];

  protected reload(): void {
    this.reload$.next();
  }

  protected open(stock: Stock): void {
    void this.router.navigate(['/stocks', stock.idStock]);
  }

  private remove(stock: Stock): void {
    this.confirm
      .confirmAndRun(
        { title: 'Delete stock', message: `Delete stock "${stock.libelleStock}"?` },
        () => this.api.delete(stock.idStock),
        'Stock deleted',
      )
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.reload());
  }
}
