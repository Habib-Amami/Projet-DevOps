import { ChangeDetectionStrategy, Component, DestroyRef, inject } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { MatButton } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';
import { Router, RouterLink } from '@angular/router';
import { BehaviorSubject, switchMap } from 'rxjs';
import { loadState } from '../../core/api/load-state';
import { CategorieProduit } from '../../core/models/categorie-produit.model';
import { CategorieProduitService } from '../../core/services/categorie-produit.service';
import { AsyncState } from '../../shared/components/async-state';
import { DataTable, TableAction, TableColumn } from '../../shared/components/data-table';
import { EmptyState } from '../../shared/components/empty-state';
import { PageHeader } from '../../shared/components/page-header';
import { ConfirmService } from '../../shared/dialogs/confirm.service';

@Component({
  selector: 'app-categorie-list',
  imports: [PageHeader, AsyncState, DataTable, EmptyState, MatButton, MatIcon, RouterLink],
  template: `
    <app-page-header title="Product categories">
      <a mat-flat-button routerLink="new"><mat-icon>add</mat-icon> New category</a>
    </app-page-header>
    <app-async-state [state]="state()" (retry)="reload()">
      @if (state().data; as categories) {
        @if (categories.length) {
          <app-data-table
            [rows]="categories"
            [columns]="columns"
            [actions]="actions"
            (rowClick)="open($event)"
          />
        } @else {
          <app-empty-state message="No product categories yet." />
        }
      }
    </app-async-state>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export default class CategorieList {
  private readonly api = inject(CategorieProduitService);
  private readonly router = inject(Router);
  private readonly confirm = inject(ConfirmService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly reload$ = new BehaviorSubject<void>(undefined);

  protected readonly state = toSignal(
    this.reload$.pipe(switchMap(() => loadState(this.api.list()))),
    { requireSync: true },
  );

  protected readonly columns: TableColumn<CategorieProduit>[] = [
    { key: 'id', header: 'ID', value: (c) => c.idCategorieProduit },
    { key: 'code', header: 'Code', value: (c) => c.codeCategorie },
    { key: 'libelle', header: 'Label', value: (c) => c.libelleCategorie },
  ];

  protected readonly actions: TableAction<CategorieProduit>[] = [
    {
      icon: 'edit',
      label: 'Edit',
      run: (c) => void this.router.navigate(['/categories-produits', c.idCategorieProduit, 'edit']),
    },
    { icon: 'delete', label: 'Delete', warn: true, run: (c) => this.remove(c) },
  ];

  protected reload(): void {
    this.reload$.next();
  }

  protected open(categorie: CategorieProduit): void {
    void this.router.navigate(['/categories-produits', categorie.idCategorieProduit]);
  }

  private remove(categorie: CategorieProduit): void {
    this.confirm
      .confirmAndRun(
        { title: 'Delete category', message: `Delete category "${categorie.libelleCategorie}"?` },
        () => this.api.delete(categorie.idCategorieProduit),
        'Category deleted',
      )
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.reload());
  }
}
