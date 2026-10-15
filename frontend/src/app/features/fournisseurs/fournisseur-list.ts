import { ChangeDetectionStrategy, Component, DestroyRef, inject } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { MatButton } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';
import { Router, RouterLink } from '@angular/router';
import { BehaviorSubject, switchMap } from 'rxjs';
import { loadState } from '../../core/api/load-state';
import { Fournisseur } from '../../core/models/fournisseur.model';
import { FournisseurService } from '../../core/services/fournisseur.service';
import { AsyncState } from '../../shared/components/async-state';
import { DataTable, TableAction, TableColumn } from '../../shared/components/data-table';
import { EmptyState } from '../../shared/components/empty-state';
import { PageHeader } from '../../shared/components/page-header';
import { ConfirmService } from '../../shared/dialogs/confirm.service';

@Component({
  selector: 'app-fournisseur-list',
  imports: [PageHeader, AsyncState, DataTable, EmptyState, MatButton, MatIcon, RouterLink],
  template: `
    <app-page-header title="Suppliers">
      <a mat-flat-button routerLink="new"><mat-icon>add</mat-icon> New supplier</a>
    </app-page-header>
    <app-async-state [state]="state()" (retry)="reload()">
      @if (state().data; as fournisseurs) {
        @if (fournisseurs.length) {
          <app-data-table
            [rows]="fournisseurs"
            [columns]="columns"
            [actions]="actions"
            (rowClick)="open($event)"
          />
        } @else {
          <app-empty-state message="No suppliers yet." />
        }
      }
    </app-async-state>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export default class FournisseurList {
  private readonly api = inject(FournisseurService);
  private readonly router = inject(Router);
  private readonly confirm = inject(ConfirmService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly reload$ = new BehaviorSubject<void>(undefined);

  protected readonly state = toSignal(
    this.reload$.pipe(switchMap(() => loadState(this.api.list()))),
    { requireSync: true },
  );

  protected readonly columns: TableColumn<Fournisseur>[] = [
    { key: 'id', header: 'ID', value: (f) => f.idFournisseur },
    { key: 'code', header: 'Code', value: (f) => f.code },
    { key: 'libelle', header: 'Name', value: (f) => f.libelle },
    { key: 'categorie', header: 'Category', value: (f) => f.categorieFournisseur },
    { key: 'secteurs', header: 'Sectors', value: (f) => f.secteurActiviteIds.length },
  ];

  protected readonly actions: TableAction<Fournisseur>[] = [
    {
      icon: 'edit',
      label: 'Edit',
      run: (f) => void this.router.navigate(['/fournisseurs', f.idFournisseur, 'edit']),
    },
    { icon: 'delete', label: 'Delete', warn: true, run: (f) => this.remove(f) },
  ];

  protected reload(): void {
    this.reload$.next();
  }

  protected open(fournisseur: Fournisseur): void {
    void this.router.navigate(['/fournisseurs', fournisseur.idFournisseur]);
  }

  private remove(fournisseur: Fournisseur): void {
    this.confirm
      .confirmAndRun(
        { title: 'Delete supplier', message: `Delete supplier "${fournisseur.libelle}"?` },
        () => this.api.delete(fournisseur.idFournisseur),
        'Supplier deleted',
      )
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.reload());
  }
}
