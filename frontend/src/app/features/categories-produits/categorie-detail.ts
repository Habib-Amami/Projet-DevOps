import { ChangeDetectionStrategy, Component, DestroyRef, inject, input } from '@angular/core';
import { takeUntilDestroyed, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { MatButton } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';
import { Router, RouterLink } from '@angular/router';
import { BehaviorSubject, combineLatest, switchMap } from 'rxjs';
import { LOADING, loadState } from '../../core/api/load-state';
import { CategorieProduit } from '../../core/models/categorie-produit.model';
import { CategorieProduitService } from '../../core/services/categorie-produit.service';
import { AsyncState } from '../../shared/components/async-state';
import { PageHeader } from '../../shared/components/page-header';
import { ConfirmService } from '../../shared/dialogs/confirm.service';

@Component({
  selector: 'app-categorie-detail',
  imports: [PageHeader, AsyncState, MatButton, MatIcon, RouterLink],
  template: `
    <app-async-state [state]="state()" (retry)="reload()">
      @if (state().data; as categorie) {
        <app-page-header [title]="categorie.libelleCategorie" backLink="/categories-produits">
          <a mat-stroked-button routerLink="edit"><mat-icon>edit</mat-icon> Edit</a>
          <button mat-flat-button class="danger" type="button" (click)="remove(categorie)">
            <mat-icon>delete</mat-icon> Delete
          </button>
        </app-page-header>
        <dl class="details">
          <dt>ID</dt>
          <dd>{{ categorie.idCategorieProduit }}</dd>
          <dt>Code</dt>
          <dd>{{ categorie.codeCategorie }}</dd>
          <dt>Label</dt>
          <dd>{{ categorie.libelleCategorie }}</dd>
        </dl>
      }
    </app-async-state>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export default class CategorieDetail {
  private readonly api = inject(CategorieProduitService);
  private readonly router = inject(Router);
  private readonly confirm = inject(ConfirmService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly reload$ = new BehaviorSubject<void>(undefined);

  readonly id = input.required<string>();

  protected readonly state = toSignal(
    combineLatest([toObservable(this.id), this.reload$]).pipe(
      switchMap(([id]) => loadState(this.api.get(Number(id)))),
    ),
    { initialValue: LOADING },
  );

  protected reload(): void {
    this.reload$.next();
  }

  protected remove(categorie: CategorieProduit): void {
    this.confirm
      .confirmAndRun(
        { title: 'Delete category', message: `Delete category "${categorie.libelleCategorie}"?` },
        () => this.api.delete(categorie.idCategorieProduit),
        'Category deleted',
      )
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => void this.router.navigate(['/categories-produits']));
  }
}
