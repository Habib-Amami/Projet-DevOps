import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  computed,
  inject,
  input,
  signal,
} from '@angular/core';
import { takeUntilDestroyed, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButton } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIcon } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { Router, RouterLink } from '@angular/router';
import { BehaviorSubject, combineLatest, finalize, forkJoin, switchMap } from 'rxjs';
import { LOADING, loadState } from '../../core/api/load-state';
import { Facture } from '../../core/models/facture.model';
import { Fournisseur } from '../../core/models/fournisseur.model';
import { FactureService } from '../../core/services/facture.service';
import { FournisseurService } from '../../core/services/fournisseur.service';
import { NotificationService } from '../../core/services/notification.service';
import { SecteurActiviteService } from '../../core/services/secteur-activite.service';
import { AsyncState } from '../../shared/components/async-state';
import { DataTable, TableColumn } from '../../shared/components/data-table';
import { EmptyState } from '../../shared/components/empty-state';
import { PageHeader } from '../../shared/components/page-header';
import { ConfirmService } from '../../shared/dialogs/confirm.service';

@Component({
  selector: 'app-fournisseur-detail',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatButton,
    MatIcon,
    MatChipsModule,
    MatFormFieldModule,
    MatSelectModule,
    PageHeader,
    AsyncState,
    DataTable,
    EmptyState,
  ],
  templateUrl: './fournisseur-detail.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export default class FournisseurDetail {
  private readonly api = inject(FournisseurService);
  private readonly secteurService = inject(SecteurActiviteService);
  private readonly factureService = inject(FactureService);
  private readonly router = inject(Router);
  private readonly confirm = inject(ConfirmService);
  private readonly notify = inject(NotificationService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly reload$ = new BehaviorSubject<void>(undefined);

  readonly id = input.required<string>();

  private readonly id$ = combineLatest([toObservable(this.id), this.reload$]);

  protected readonly state = toSignal(
    this.id$.pipe(
      switchMap(([id]) =>
        loadState(
          forkJoin({
            fournisseur: this.api.get(Number(id)),
            secteurs: this.secteurService.list(),
          }),
        ),
      ),
    ),
    { initialValue: LOADING },
  );

  protected readonly factures = toSignal(
    this.id$.pipe(
      switchMap(([id]) => loadState(this.factureService.listByFournisseur(Number(id)))),
    ),
    { initialValue: LOADING },
  );

  protected readonly assignedSecteurs = computed(() => {
    const data = this.state().data;
    return data
      ? data.secteurs.filter((s) =>
          data.fournisseur.secteurActiviteIds.includes(s.idSecteurActivite),
        )
      : [];
  });

  protected readonly availableSecteurs = computed(() => {
    const data = this.state().data;
    return data
      ? data.secteurs.filter(
          (s) => !data.fournisseur.secteurActiviteIds.includes(s.idSecteurActivite),
        )
      : [];
  });

  protected readonly secteurControl = new FormControl<number | null>(null, Validators.required);
  protected readonly assigning = signal(false);

  protected readonly factureColumns: TableColumn<Facture>[] = [
    { key: 'id', header: 'ID', value: (f) => f.idFacture },
    { key: 'date', header: 'Date', value: (f) => f.dateCreationFacture, type: 'date' },
    { key: 'montant', header: 'Amount', value: (f) => f.montantFacture, type: 'amount' },
    { key: 'archivee', header: 'Cancelled', value: (f) => f.archivee === true, type: 'boolean' },
  ];

  protected reload(): void {
    this.reload$.next();
  }

  protected openFacture(facture: Facture): void {
    void this.router.navigate(['/factures', facture.idFacture]);
  }

  protected assignSecteur(fournisseur: Fournisseur): void {
    const secteurId = this.secteurControl.value;
    if (secteurId === null) {
      return;
    }
    this.assigning.set(true);
    this.api
      .assignSecteurActivite(fournisseur.idFournisseur, secteurId)
      .pipe(
        finalize(() => this.assigning.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: () => {
          this.notify.success('Sector assigned');
          this.secteurControl.reset();
          this.reload();
        },
        error: (error: unknown) => this.notify.error(error),
      });
  }

  protected remove(fournisseur: Fournisseur): void {
    this.confirm
      .confirmAndRun(
        { title: 'Delete supplier', message: `Delete supplier "${fournisseur.libelle}"?` },
        () => this.api.delete(fournisseur.idFournisseur),
        'Supplier deleted',
      )
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => void this.router.navigate(['/fournisseurs']));
  }
}
