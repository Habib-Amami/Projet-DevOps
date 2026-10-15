import { ChangeDetectionStrategy, Component, DestroyRef, inject } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInput } from '@angular/material/input';
import { Router } from '@angular/router';
import {
  BehaviorSubject,
  combineLatest,
  debounceTime,
  distinctUntilChanged,
  startWith,
  switchMap,
} from 'rxjs';
import { LOADING, loadState } from '../../core/api/load-state';
import { Reglement } from '../../core/models/reglement.model';
import { ReglementService } from '../../core/services/reglement.service';
import { AsyncState } from '../../shared/components/async-state';
import { DataTable, TableAction, TableColumn } from '../../shared/components/data-table';
import { DateRangeQuery, DateRangeStat } from '../../shared/components/date-range-stat';
import { EmptyState } from '../../shared/components/empty-state';
import { PageHeader } from '../../shared/components/page-header';
import { ConfirmService } from '../../shared/dialogs/confirm.service';

@Component({
  selector: 'app-reglement-list',
  imports: [
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInput,
    PageHeader,
    AsyncState,
    DataTable,
    DateRangeStat,
    EmptyState,
  ],
  template: `
    <app-page-header title="Payments" />
    <div class="stats">
      <app-date-range-stat title="Revenue over a period" [query]="revenue" />
    </div>
    <div class="toolbar-row">
      <mat-form-field subscriptSizing="dynamic">
        <mat-label>Filter by invoice ID</mat-label>
        <input matInput type="number" min="1" [formControl]="factureFilter" />
      </mat-form-field>
    </div>
    <app-async-state [state]="state()" (retry)="reload()">
      @if (state().data; as reglements) {
        @if (reglements.length) {
          <app-data-table
            [rows]="reglements"
            [columns]="columns"
            [actions]="actions"
            (rowClick)="open($event)"
          />
        } @else {
          <app-empty-state message="No payments found." />
        }
      }
    </app-async-state>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export default class ReglementList {
  private readonly api = inject(ReglementService);
  private readonly router = inject(Router);
  private readonly confirm = inject(ConfirmService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly reload$ = new BehaviorSubject<void>(undefined);

  protected readonly factureFilter = new FormControl<number | null>(null, Validators.min(1));

  protected readonly state = toSignal(
    combineLatest([
      this.factureFilter.valueChanges.pipe(
        startWith(null),
        debounceTime(300),
        distinctUntilChanged(),
      ),
      this.reload$,
    ]).pipe(
      switchMap(([factureId]) =>
        loadState(factureId ? this.api.listByFacture(factureId) : this.api.list()),
      ),
    ),
    { initialValue: LOADING },
  );

  protected readonly revenue: DateRangeQuery = (start, end) => this.api.revenue(start, end);

  protected readonly columns: TableColumn<Reglement>[] = [
    { key: 'id', header: 'ID', value: (r) => r.idReglement },
    { key: 'date', header: 'Date', value: (r) => r.dateReglement, type: 'date' },
    { key: 'paye', header: 'Paid', value: (r) => r.montantPaye, type: 'amount' },
    { key: 'restant', header: 'Remaining', value: (r) => r.montantRestant, type: 'amount' },
    { key: 'payee', header: 'Settled', value: (r) => r.payee, type: 'boolean' },
    { key: 'facture', header: 'Invoice', value: (r) => r.factureId },
  ];

  protected readonly actions: TableAction<Reglement>[] = [
    { icon: 'delete', label: 'Delete', warn: true, run: (r) => this.remove(r) },
  ];

  protected reload(): void {
    this.reload$.next();
  }

  protected open(reglement: Reglement): void {
    void this.router.navigate(['/reglements', reglement.idReglement]);
  }

  private remove(reglement: Reglement): void {
    this.confirm
      .confirmAndRun(
        { title: 'Delete payment', message: `Delete payment #${reglement.idReglement}?` },
        () => this.api.delete(reglement.idReglement),
        'Payment deleted',
      )
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.reload());
  }
}
