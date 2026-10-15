import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  OnInit,
  inject,
  input,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButton } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInput } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { Router, RouterLink } from '@angular/router';
import { Observable, finalize, forkJoin, of, switchMap } from 'rxjs';
import { LoadState, loadState } from '../../core/api/load-state';
import { Produit, ProduitPayload } from '../../core/models/produit.model';
import { Stock } from '../../core/models/stock.model';
import { NotificationService } from '../../core/services/notification.service';
import { ProduitService } from '../../core/services/produit.service';
import { StockService } from '../../core/services/stock.service';
import { AsyncState } from '../../shared/components/async-state';
import { PageHeader } from '../../shared/components/page-header';
import { fieldError } from '../../shared/forms/field-error';
import { notBlank } from '../../shared/forms/validators';

interface FormContext {
  stocks: Stock[];
  produit: Produit | null;
}

@Component({
  selector: 'app-produit-form',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatButton,
    MatFormFieldModule,
    MatInput,
    MatSelectModule,
    PageHeader,
    AsyncState,
  ],
  template: `
    <app-page-header [title]="id() ? 'Edit product' : 'New product'" backLink="/produits" />
    <app-async-state [state]="context()" (retry)="load()">
      <form class="form" [formGroup]="form" (ngSubmit)="submit()">
        <div class="form-row">
          <mat-form-field>
            <mat-label>Code</mat-label>
            <input matInput formControlName="codeProduit" />
            <mat-error>{{ fieldError(form.controls.codeProduit) }}</mat-error>
          </mat-form-field>
          <mat-form-field>
            <mat-label>Price</mat-label>
            <input matInput type="number" min="0" step="0.01" formControlName="prix" />
            <mat-error>{{ fieldError(form.controls.prix) }}</mat-error>
          </mat-form-field>
        </div>
        <mat-form-field>
          <mat-label>Label</mat-label>
          <input matInput formControlName="libelleProduit" />
          <mat-error>{{ fieldError(form.controls.libelleProduit) }}</mat-error>
        </mat-form-field>
        <mat-form-field>
          <mat-label>Stock</mat-label>
          <mat-select formControlName="stockId">
            @if (!context().data?.produit?.stockId) {
              <mat-option [value]="null">None</mat-option>
            }
            @for (stock of context().data?.stocks; track stock.idStock) {
              <mat-option [value]="stock.idStock">{{ stock.libelleStock }}</mat-option>
            }
          </mat-select>
        </mat-form-field>
        <div class="form-actions">
          <button mat-flat-button type="submit" [disabled]="saving()">Save</button>
          <a mat-button routerLink="/produits">Cancel</a>
        </div>
      </form>
    </app-async-state>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export default class ProduitForm implements OnInit {
  private readonly api = inject(ProduitService);
  private readonly stockService = inject(StockService);
  private readonly router = inject(Router);
  private readonly notify = inject(NotificationService);
  private readonly destroyRef = inject(DestroyRef);

  readonly id = input<string>();

  protected readonly fieldError = fieldError;
  protected readonly saving = signal(false);
  protected readonly context = signal<LoadState<FormContext>>({
    loading: true,
    data: null,
    error: null,
  });

  protected readonly form = inject(FormBuilder).nonNullable.group({
    codeProduit: ['', [Validators.required, notBlank]],
    libelleProduit: ['', [Validators.required, notBlank]],
    prix: [0, [Validators.required, Validators.min(0)]],
    stockId: [null as number | null],
  });

  ngOnInit(): void {
    this.load();
  }

  protected load(): void {
    const id = this.id();
    loadState(
      forkJoin({
        stocks: this.stockService.list(),
        produit: id ? this.api.get(Number(id)) : of(null),
      }),
    )
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((state) => {
        this.context.set(state);
        const produit = state.data?.produit;
        if (produit) {
          const { codeProduit, libelleProduit, prix, stockId } = produit;
          this.form.setValue({ codeProduit, libelleProduit, prix, stockId });
        }
      });
  }

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const id = this.id();
    const { stockId, ...payload } = this.form.getRawValue();
    this.saving.set(true);
    this.save(id, payload)
      .pipe(
        switchMap((produit) =>
          stockId !== null && stockId !== produit.stockId
            ? this.api.assignStock(produit.idProduit, stockId)
            : of(produit),
        ),
        finalize(() => this.saving.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (produit) => {
          this.notify.success(id ? 'Product updated' : 'Product created');
          void this.router.navigate(['/produits', produit.idProduit]);
        },
        error: (error: unknown) => this.notify.error(error),
      });
  }

  private save(id: string | undefined, payload: ProduitPayload): Observable<Produit> {
    return id ? this.api.update(Number(id), payload) : this.api.create(payload);
  }
}
