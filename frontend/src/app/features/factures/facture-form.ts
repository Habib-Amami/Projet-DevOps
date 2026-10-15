import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButton } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInput } from '@angular/material/input';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { FactureService } from '../../core/services/facture.service';
import { NotificationService } from '../../core/services/notification.service';
import { PageHeader } from '../../shared/components/page-header';
import { fieldError } from '../../shared/forms/field-error';
import { toIsoDate } from '../../shared/utils/date';

/** Invoices can only be created: the backend exposes no update or delete endpoint. */
@Component({
  selector: 'app-facture-form',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatButton,
    MatDatepickerModule,
    MatFormFieldModule,
    MatInput,
    PageHeader,
  ],
  template: `
    <app-page-header title="New invoice" backLink="/factures" />
    <form class="form" [formGroup]="form" (ngSubmit)="submit()">
      <mat-form-field>
        <mat-label>Creation date</mat-label>
        <input matInput [matDatepicker]="picker" formControlName="dateCreationFacture" />
        <mat-datepicker-toggle matIconSuffix [for]="picker" />
        <mat-datepicker #picker />
        <mat-error>{{ fieldError(form.controls.dateCreationFacture) }}</mat-error>
      </mat-form-field>
      <div class="form-row">
        <mat-form-field>
          <mat-label>Amount</mat-label>
          <input matInput type="number" min="0" step="0.01" formControlName="montantFacture" />
          <mat-error>{{ fieldError(form.controls.montantFacture) }}</mat-error>
        </mat-form-field>
        <mat-form-field>
          <mat-label>Discount</mat-label>
          <input matInput type="number" min="0" step="0.01" formControlName="montantRemise" />
          <mat-error>{{ fieldError(form.controls.montantRemise) }}</mat-error>
        </mat-form-field>
      </div>
      <div class="form-actions">
        <button mat-flat-button type="submit" [disabled]="saving()">Save</button>
        <a mat-button routerLink="/factures">Cancel</a>
      </div>
    </form>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export default class FactureForm {
  private readonly api = inject(FactureService);
  private readonly router = inject(Router);
  private readonly notify = inject(NotificationService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly fieldError = fieldError;
  protected readonly saving = signal(false);

  protected readonly form = inject(FormBuilder).nonNullable.group({
    dateCreationFacture: [new Date(), Validators.required],
    montantFacture: [0, [Validators.required, Validators.min(0)]],
    montantRemise: [0, [Validators.required, Validators.min(0)]],
  });

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { dateCreationFacture, montantFacture, montantRemise } = this.form.getRawValue();
    this.saving.set(true);
    this.api
      .create({
        dateCreationFacture: toIsoDate(dateCreationFacture),
        montantFacture,
        montantRemise,
        archivee: false,
      })
      .pipe(
        finalize(() => this.saving.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (facture) => {
          this.notify.success('Invoice created');
          void this.router.navigate(['/factures', facture.idFacture]);
        },
        error: (error: unknown) => this.notify.error(error),
      });
  }
}
