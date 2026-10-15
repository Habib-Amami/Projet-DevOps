import { ChangeDetectionStrategy, Component, DestroyRef, inject, input } from '@angular/core';
import { takeUntilDestroyed, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { MatButton } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';
import { Router, RouterLink } from '@angular/router';
import { BehaviorSubject, combineLatest, switchMap } from 'rxjs';
import { LOADING, loadState } from '../../core/api/load-state';
import { SecteurActivite } from '../../core/models/secteur-activite.model';
import { SecteurActiviteService } from '../../core/services/secteur-activite.service';
import { AsyncState } from '../../shared/components/async-state';
import { PageHeader } from '../../shared/components/page-header';
import { ConfirmService } from '../../shared/dialogs/confirm.service';

@Component({
  selector: 'app-secteur-detail',
  imports: [PageHeader, AsyncState, MatButton, MatIcon, RouterLink],
  template: `
    <app-async-state [state]="state()" (retry)="reload()">
      @if (state().data; as secteur) {
        <app-page-header [title]="secteur.libelleSecteurActivite" backLink="/secteurs-activite">
          <a mat-stroked-button routerLink="edit"><mat-icon>edit</mat-icon> Edit</a>
          <button mat-flat-button class="danger" type="button" (click)="remove(secteur)">
            <mat-icon>delete</mat-icon> Delete
          </button>
        </app-page-header>
        <dl class="details">
          <dt>ID</dt>
          <dd>{{ secteur.idSecteurActivite }}</dd>
          <dt>Code</dt>
          <dd>{{ secteur.codeSecteurActivite }}</dd>
          <dt>Label</dt>
          <dd>{{ secteur.libelleSecteurActivite }}</dd>
        </dl>
      }
    </app-async-state>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export default class SecteurDetail {
  private readonly api = inject(SecteurActiviteService);
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

  protected remove(secteur: SecteurActivite): void {
    this.confirm
      .confirmAndRun(
        { title: 'Delete sector', message: `Delete sector "${secteur.libelleSecteurActivite}"?` },
        () => this.api.delete(secteur.idSecteurActivite),
        'Sector deleted',
      )
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => void this.router.navigate(['/secteurs-activite']));
  }
}
