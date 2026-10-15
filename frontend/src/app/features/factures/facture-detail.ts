import { DatePipe, DecimalPipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  inject,
  input,
  signal,
} from '@angular/core';
import { takeUntilDestroyed, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButton } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIcon } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { Router } from '@angular/router';
import { BehaviorSubject, combineLatest, finalize, switchMap } from 'rxjs';
import { LOADING, loadState } from '../../core/api/load-state';
import { DetailFacture, Facture } from '../../core/models/facture.model';
import { Reglement } from '../../core/models/reglement.model';
import { FactureService } from '../../core/services/facture.service';
import { NotificationService } from '../../core/services/notification.service';
import { OperateurService } from '../../core/services/operateur.service';
import { ReglementService } from '../../core/services/reglement.service';
import { AsyncState } from '../../shared/components/async-state';
import { DataTable, TableColumn } from '../../shared/components/data-table';
import { EmptyState } from '../../shared/components/empty-state';
import { PageHeader } from '../../shared/components/page-header';
import { ConfirmService } from '../../shared/dialogs/confirm.service';

@Component({
  selector: 'app-facture-detail',
  imports: [
    ReactiveFormsModule,
    DatePipe,
    DecimalPipe,
    MatButton,
    MatIcon,
    MatFormFieldModule,
    MatSelectModule,
    PageHeader,
    AsyncState,
    DataTable,
    EmptyState,
  ],
  templateUrl: './facture-detail.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export default class FactureDetail {
  private readonly api = inject(FactureService);
  private readonly reglementService = inject(ReglementService);
  private readonly operateurService = inject(OperateurService);
  private readonly router = inject(Router);
  private readonly confirm = inject(ConfirmService);
  private readonly notify = inject(NotificationService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly reload$ = new BehaviorSubject<void>(undefined);

  readonly id = input.required<string>();

  private readonly id$ = combineLatest([toObservable(this.id), this.reload$]);

  protected readonly state = toSignal(
    this.id$.pipe(switchMap(([id]) => loadState(this.api.get(Number(id))))),
    { initialValue: LOADING },
  );

  protected readonly reglements = toSignal(
    this.id$.pipe(switchMap(([id]) => loadState(this.reglementService.listByFacture(Number(id))))),
    { initialValue: LOADING },
  );

  protected readonly operateurs = toSignal(loadState(this.operateurService.list()), {
    initialValue: LOADING,
  });

  protected readonly operateurControl = new FormControl<number | null>(null, Validators.required);
  protected readonly assigning = signal(false);

  protected readonly detailColumns: TableColumn<DetailFacture>[] = [
    { key: 'produit', header: 'Product', value: (d) => d.produit?.libelleProduit ?? null },
    { key: 'qte', header: 'Quantity', value: (d) => d.qteCommandee },
    { key: 'pourcentage', header: 'Discount %', value: (d) => d.pourcentageRemise },
    { key: 'remise', header: 'Discount', value: (d) => d.montantRemise, type: 'amount' },
    { key: 'total', header: 'Total', value: (d) => d.prixTotalDetail, type: 'amount' },
  ];

  protected readonly reglementColumns: TableColumn<Reglement>[] = [
    { key: 'id', header: 'ID', value: (r) => r.idReglement },
    { key: 'date', header: 'Date', value: (r) => r.dateReglement, type: 'date' },
    { key: 'paye', header: 'Paid', value: (r) => r.montantPaye, type: 'amount' },
    { key: 'restant', header: 'Remaining', value: (r) => r.montantRestant, type: 'amount' },
    { key: 'payee', header: 'Settled', value: (r) => r.payee, type: 'boolean' },
  ];

  protected reload(): void {
    this.reload$.next();
  }

  protected openProduit(detail: DetailFacture): void {
    if (detail.produit) {
      void this.router.navigate(['/produits', detail.produit.idProduit]);
    }
  }

  protected openReglement(reglement: Reglement): void {
    void this.router.navigate(['/reglements', reglement.idReglement]);
  }

  protected assignOperateur(facture: Facture): void {
    const operateurId = this.operateurControl.value;
    if (operateurId === null) {
      return;
    }
    this.assigning.set(true);
    this.api
      .assignOperateur(operateurId, facture.idFacture)
      .pipe(
        finalize(() => this.assigning.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: () => {
          this.notify.success('Operator assigned');
          this.operateurControl.reset();
        },
        error: (error: unknown) => this.notify.error(error),
      });
  }

  protected cancel(facture: Facture): void {
    this.confirm
      .confirmAndRun(
        {
          title: 'Cancel invoice',
          message: `Cancel invoice #${facture.idFacture}? It will be archived.`,
          confirmLabel: 'Cancel invoice',
        },
        () => this.api.cancel(facture.idFacture),
        'Invoice cancelled',
      )
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.reload());
  }
}
