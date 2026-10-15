import { ChangeDetectionStrategy, Component, DestroyRef, inject } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { MatButton } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';
import { Router, RouterLink } from '@angular/router';
import { BehaviorSubject, switchMap } from 'rxjs';
import { loadState } from '../../core/api/load-state';
import { Facture } from '../../core/models/facture.model';
import { FactureService } from '../../core/services/facture.service';
import { AsyncState } from '../../shared/components/async-state';
import { DataTable, TableAction, TableColumn } from '../../shared/components/data-table';
import { DateRangeQuery, DateRangeStat } from '../../shared/components/date-range-stat';
import { EmptyState } from '../../shared/components/empty-state';
import { PageHeader } from '../../shared/components/page-header';
import { ConfirmService } from '../../shared/dialogs/confirm.service';

@Component({
  selector: 'app-facture-list',
  imports: [
    PageHeader,
    AsyncState,
    DataTable,
    DateRangeStat,
    EmptyState,
    MatButton,
    MatIcon,
    RouterLink,
  ],
  template: `
    <app-page-header title="Invoices">
      <a mat-flat-button routerLink="new"><mat-icon>add</mat-icon> New invoice</a>
    </app-page-header>
    <div class="stats">
      <app-date-range-stat title="Recovery rate" suffix=" %" [query]="recoveryRate" />
    </div>
    <app-async-state [state]="state()" (retry)="reload()">
      @if (state().data; as factures) {
        @if (factures.length) {
          <app-data-table
            [rows]="factures"
            [columns]="columns"
            [actions]="actions"
            (rowClick)="open($event)"
          />
        } @else {
          <app-empty-state message="No invoices yet." />
        }
      }
    </app-async-state>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export default class FactureList {
  private readonly api = inject(FactureService);
  private readonly router = inject(Router);
  private readonly confirm = inject(ConfirmService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly reload$ = new BehaviorSubject<void>(undefined);

  protected readonly state = toSignal(
    this.reload$.pipe(switchMap(() => loadState(this.api.list()))),
    { requireSync: true },
  );

  protected readonly recoveryRate: DateRangeQuery = (start, end) =>
    this.api.recoveryRate(start, end);

  protected readonly columns: TableColumn<Facture>[] = [
    { key: 'id', header: 'ID', value: (f) => f.idFacture },
    { key: 'date', header: 'Date', value: (f) => f.dateCreationFacture, type: 'date' },
    { key: 'montant', header: 'Amount', value: (f) => f.montantFacture, type: 'amount' },
    { key: 'remise', header: 'Discount', value: (f) => f.montantRemise, type: 'amount' },
    { key: 'archivee', header: 'Cancelled', value: (f) => f.archivee === true, type: 'boolean' },
  ];

  protected readonly actions: TableAction<Facture>[] = [
    {
      icon: 'block',
      label: 'Cancel invoice',
      warn: true,
      visible: (f) => !f.archivee,
      run: (f) => this.cancel(f),
    },
  ];

  protected reload(): void {
    this.reload$.next();
  }

  protected open(facture: Facture): void {
    void this.router.navigate(['/factures', facture.idFacture]);
  }

  private cancel(facture: Facture): void {
    this.confirm
      .confirmAndRun(
        {
          title: 'Cancel invoice',
          message: `Cancel invoice #${facture.idFacture}? It will be archived.`,
          confirmLabel: 'Cancel invoice',
        },
        () => this.api.cancel(facture.idFacture),
        'Invoice cancelled',
      )
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.reload());
  }
}
