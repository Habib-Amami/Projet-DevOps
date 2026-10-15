import { DatePipe, DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, inject, input } from '@angular/core';
import { takeUntilDestroyed, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { MatButton } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';
import { Router, RouterLink } from '@angular/router';
import { BehaviorSubject, combineLatest, switchMap } from 'rxjs';
import { LOADING, loadState } from '../../core/api/load-state';
import { Reglement } from '../../core/models/reglement.model';
import { ReglementService } from '../../core/services/reglement.service';
import { AsyncState } from '../../shared/components/async-state';
import { PageHeader } from '../../shared/components/page-header';
import { ConfirmService } from '../../shared/dialogs/confirm.service';

@Component({
  selector: 'app-reglement-detail',
  imports: [PageHeader, AsyncState, MatButton, MatIcon, RouterLink, DatePipe, DecimalPipe],
  template: `
    <app-async-state [state]="state()" (retry)="reload()">
      @if (state().data; as reglement) {
        <app-page-header [title]="'Payment #' + reglement.idReglement" backLink="/reglements">
          <button mat-flat-button class="danger" type="button" (click)="remove(reglement)">
            <mat-icon>delete</mat-icon> Delete
          </button>
        </app-page-header>
        <dl class="details">
          <dt>Date</dt>
          <dd>{{ reglement.dateReglement | date: 'mediumDate' }}</dd>
          <dt>Paid</dt>
          <dd>{{ reglement.montantPaye | number: '1.2-2' }}</dd>
          <dt>Remaining</dt>
          <dd>{{ reglement.montantRestant | number: '1.2-2' }}</dd>
          <dt>Settled</dt>
          <dd>{{ reglement.payee ? 'Yes' : 'No' }}</dd>
          <dt>Invoice</dt>
          <dd>
            @if (reglement.factureId; as factureId) {
              <a [routerLink]="['/factures', factureId]">#{{ factureId }}</a>
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
export default class ReglementDetail {
  private readonly api = inject(ReglementService);
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

  protected remove(reglement: Reglement): void {
    this.confirm
      .confirmAndRun(
        { title: 'Delete payment', message: `Delete payment #${reglement.idReglement}?` },
        () => this.api.delete(reglement.idReglement),
        'Payment deleted',
      )
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => void this.router.navigate(['/reglements']));
  }
}
