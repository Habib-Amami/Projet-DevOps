import { ChangeDetectionStrategy, Component, DestroyRef, inject, input } from '@angular/core';
import { takeUntilDestroyed, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { MatButton } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';
import { Router, RouterLink } from '@angular/router';
import { BehaviorSubject, combineLatest, switchMap } from 'rxjs';
import { LOADING, loadState } from '../../core/api/load-state';
import { Stock } from '../../core/models/stock.model';
import { StockService } from '../../core/services/stock.service';
import { AsyncState } from '../../shared/components/async-state';
import { PageHeader } from '../../shared/components/page-header';
import { ConfirmService } from '../../shared/dialogs/confirm.service';

@Component({
  selector: 'app-stock-detail',
  imports: [PageHeader, AsyncState, MatButton, MatIcon, RouterLink],
  template: `
    <app-async-state [state]="state()" (retry)="reload()">
      @if (state().data; as stock) {
        <app-page-header [title]="stock.libelleStock" backLink="/stocks">
          <a mat-stroked-button routerLink="edit"><mat-icon>edit</mat-icon> Edit</a>
          <button mat-flat-button class="danger" type="button" (click)="remove(stock)">
            <mat-icon>delete</mat-icon> Delete
          </button>
        </app-page-header>
        <dl class="details">
          <dt>ID</dt>
          <dd>{{ stock.idStock }}</dd>
          <dt>Label</dt>
          <dd>{{ stock.libelleStock }}</dd>
          <dt>Quantity</dt>
          <dd>{{ stock.qte }}</dd>
          <dt>Min. quantity</dt>
          <dd>{{ stock.qteMin }}</dd>
          <dt>Below minimum</dt>
          <dd>{{ stock.qte < stock.qteMin ? 'Yes' : 'No' }}</dd>
        </dl>
      }
    </app-async-state>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export default class StockDetail {
  private readonly api = inject(StockService);
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

  protected remove(stock: Stock): void {
    this.confirm
      .confirmAndRun(
        { title: 'Delete stock', message: `Delete stock "${stock.libelleStock}"?` },
        () => this.api.delete(stock.idStock),
        'Stock deleted',
      )
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => void this.router.navigate(['/stocks']));
  }
}
