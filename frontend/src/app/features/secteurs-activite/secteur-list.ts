import { ChangeDetectionStrategy, Component, DestroyRef, inject } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { MatButton } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';
import { Router, RouterLink } from '@angular/router';
import { BehaviorSubject, switchMap } from 'rxjs';
import { loadState } from '../../core/api/load-state';
import { SecteurActivite } from '../../core/models/secteur-activite.model';
import { SecteurActiviteService } from '../../core/services/secteur-activite.service';
import { AsyncState } from '../../shared/components/async-state';
import { DataTable, TableAction, TableColumn } from '../../shared/components/data-table';
import { EmptyState } from '../../shared/components/empty-state';
import { PageHeader } from '../../shared/components/page-header';
import { ConfirmService } from '../../shared/dialogs/confirm.service';

@Component({
  selector: 'app-secteur-list',
  imports: [PageHeader, AsyncState, DataTable, EmptyState, MatButton, MatIcon, RouterLink],
  template: `
    <app-page-header title="Business sectors">
      <a mat-flat-button routerLink="new"><mat-icon>add</mat-icon> New sector</a>
    </app-page-header>
    <app-async-state [state]="state()" (retry)="reload()">
      @if (state().data; as secteurs) {
        @if (secteurs.length) {
          <app-data-table
            [rows]="secteurs"
            [columns]="columns"
            [actions]="actions"
            (rowClick)="open($event)"
          />
        } @else {
          <app-empty-state message="No business sectors yet." />
        }
      }
    </app-async-state>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export default class SecteurList {
  private readonly api = inject(SecteurActiviteService);
  private readonly router = inject(Router);
  private readonly confirm = inject(ConfirmService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly reload$ = new BehaviorSubject<void>(undefined);

  protected readonly state = toSignal(
    this.reload$.pipe(switchMap(() => loadState(this.api.list()))),
    { requireSync: true },
  );

  protected readonly columns: TableColumn<SecteurActivite>[] = [
    { key: 'id', header: 'ID', value: (c) => c.idSecteurActivite },
    { key: 'code', header: 'Code', value: (c) => c.codeSecteurActivite },
    { key: 'libelle', header: 'Label', value: (c) => c.libelleSecteurActivite },
  ];

  protected readonly actions: TableAction<SecteurActivite>[] = [
    {
      icon: 'edit',
      label: 'Edit',
      run: (c) => void this.router.navigate(['/secteurs-activite', c.idSecteurActivite, 'edit']),
    },
    { icon: 'delete', label: 'Delete', warn: true, run: (c) => this.remove(c) },
  ];

  protected reload(): void {
    this.reload$.next();
  }

  protected open(secteur: SecteurActivite): void {
    void this.router.navigate(['/secteurs-activite', secteur.idSecteurActivite]);
  }

  private remove(secteur: SecteurActivite): void {
    this.confirm
      .confirmAndRun(
        { title: 'Delete sector', message: `Delete sector "${secteur.libelleSecteurActivite}"?` },
        () => this.api.delete(secteur.idSecteurActivite),
        'Sector deleted',
      )
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.reload());
  }
}
