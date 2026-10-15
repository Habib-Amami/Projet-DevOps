import { ChangeDetectionStrategy, Component, DestroyRef, inject } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { MatButton } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';
import { Router, RouterLink } from '@angular/router';
import { BehaviorSubject, switchMap } from 'rxjs';
import { loadState } from '../../core/api/load-state';
import { Operateur } from '../../core/models/operateur.model';
import { OperateurService } from '../../core/services/operateur.service';
import { AsyncState } from '../../shared/components/async-state';
import { DataTable, TableAction, TableColumn } from '../../shared/components/data-table';
import { EmptyState } from '../../shared/components/empty-state';
import { PageHeader } from '../../shared/components/page-header';
import { ConfirmService } from '../../shared/dialogs/confirm.service';

@Component({
  selector: 'app-operateur-list',
  imports: [PageHeader, AsyncState, DataTable, EmptyState, MatButton, MatIcon, RouterLink],
  template: `
    <app-page-header title="Operators">
      <a mat-flat-button routerLink="new"><mat-icon>add</mat-icon> New operator</a>
    </app-page-header>
    <app-async-state [state]="state()" (retry)="reload()">
      @if (state().data; as operateurs) {
        @if (operateurs.length) {
          <app-data-table
            [rows]="operateurs"
            [columns]="columns"
            [actions]="actions"
            (rowClick)="open($event)"
          />
        } @else {
          <app-empty-state message="No operators yet." />
        }
      }
    </app-async-state>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export default class OperateurList {
  private readonly api = inject(OperateurService);
  private readonly router = inject(Router);
  private readonly confirm = inject(ConfirmService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly reload$ = new BehaviorSubject<void>(undefined);

  protected readonly state = toSignal(
    this.reload$.pipe(switchMap(() => loadState(this.api.list()))),
    { requireSync: true },
  );

  protected readonly columns: TableColumn<Operateur>[] = [
    { key: 'id', header: 'ID', value: (o) => o.idOperateur },
    { key: 'prenom', header: 'First name', value: (o) => o.prenom },
    { key: 'nom', header: 'Last name', value: (o) => o.nom },
    { key: 'factures', header: 'Invoices', value: (o) => o.factureIds.length },
  ];

  protected readonly actions: TableAction<Operateur>[] = [
    {
      icon: 'edit',
      label: 'Edit',
      run: (o) => void this.router.navigate(['/operateurs', o.idOperateur, 'edit']),
    },
    { icon: 'delete', label: 'Delete', warn: true, run: (o) => this.remove(o) },
  ];

  protected reload(): void {
    this.reload$.next();
  }

  protected open(operateur: Operateur): void {
    void this.router.navigate(['/operateurs', operateur.idOperateur]);
  }

  private remove(operateur: Operateur): void {
    this.confirm
      .confirmAndRun(
        {
          title: 'Delete operator',
          message: `Delete operator "${operateur.prenom} ${operateur.nom}"?`,
        },
        () => this.api.delete(operateur.idOperateur),
        'Operator deleted',
      )
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.reload());
  }
}
