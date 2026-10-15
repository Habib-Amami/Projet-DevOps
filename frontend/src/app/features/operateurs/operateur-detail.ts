import { ChangeDetectionStrategy, Component, DestroyRef, inject, input } from '@angular/core';
import { takeUntilDestroyed, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { MatButton } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';
import { Router, RouterLink } from '@angular/router';
import { BehaviorSubject, combineLatest, switchMap } from 'rxjs';
import { LOADING, loadState } from '../../core/api/load-state';
import { Operateur } from '../../core/models/operateur.model';
import { OperateurService } from '../../core/services/operateur.service';
import { AsyncState } from '../../shared/components/async-state';
import { PageHeader } from '../../shared/components/page-header';
import { ConfirmService } from '../../shared/dialogs/confirm.service';

@Component({
  selector: 'app-operateur-detail',
  imports: [PageHeader, AsyncState, MatButton, MatIcon, RouterLink],
  template: `
    <app-async-state [state]="state()" (retry)="reload()">
      @if (state().data; as operateur) {
        <app-page-header [title]="operateur.prenom + ' ' + operateur.nom" backLink="/operateurs">
          <a mat-stroked-button routerLink="edit"><mat-icon>edit</mat-icon> Edit</a>
          <button mat-flat-button class="danger" type="button" (click)="remove(operateur)">
            <mat-icon>delete</mat-icon> Delete
          </button>
        </app-page-header>
        <dl class="details">
          <dt>ID</dt>
          <dd>{{ operateur.idOperateur }}</dd>
          <dt>First name</dt>
          <dd>{{ operateur.prenom }}</dd>
          <dt>Last name</dt>
          <dd>{{ operateur.nom }}</dd>
          <dt>Invoices</dt>
          <dd>
            @for (factureId of operateur.factureIds; track factureId; let last = $last) {
              <a [routerLink]="['/factures', factureId]">#{{ factureId }}</a
              >{{ last ? '' : ', ' }}
            } @empty {
              None
            }
          </dd>
        </dl>
      }
    </app-async-state>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export default class OperateurDetail {
  private readonly api = inject(OperateurService);
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

  protected remove(operateur: Operateur): void {
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
      .subscribe(() => void this.router.navigate(['/operateurs']));
  }
}
