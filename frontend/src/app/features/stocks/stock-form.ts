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
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { LoadState, loadState } from '../../core/api/load-state';
import { Stock } from '../../core/models/stock.model';
import { NotificationService } from '../../core/services/notification.service';
import { StockService } from '../../core/services/stock.service';
import { AsyncState } from '../../shared/components/async-state';
import { PageHeader } from '../../shared/components/page-header';
import { fieldError } from '../../shared/forms/field-error';
import { integer, notBlank } from '../../shared/forms/validators';

@Component({
  selector: 'app-stock-form',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatButton,
    MatFormFieldModule,
    MatInput,
    PageHeader,
    AsyncState,
  ],
  template: `
    <app-page-header [title]="id() ? 'Edit stock' : 'New stock'" backLink="/stocks" />
    <app-async-state [state]="existing()" (retry)="load()">
      <form class="form" [formGroup]="form" (ngSubmit)="submit()">
        <mat-form-field>
          <mat-label>Label</mat-label>
          <input matInput formControlName="libelleStock" />
          <mat-error>{{ fieldError(form.controls.libelleStock) }}</mat-error>
        </mat-form-field>
        <div class="form-row">
          <mat-form-field>
            <mat-label>Quantity</mat-label>
            <input matInput type="number" min="0" step="1" formControlName="qte" />
            <mat-error>{{ fieldError(form.controls.qte) }}</mat-error>
          </mat-form-field>
          <mat-form-field>
            <mat-label>Min. quantity</mat-label>
            <input matInput type="number" min="0" step="1" formControlName="qteMin" />
            <mat-error>{{ fieldError(form.controls.qteMin) }}</mat-error>
          </mat-form-field>
        </div>
        <div class="form-actions">
          <button mat-flat-button type="submit" [disabled]="saving()">Save</button>
          <a mat-button routerLink="/stocks">Cancel</a>
        </div>
      </form>
    </app-async-state>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export default class StockForm implements OnInit {
  private readonly api = inject(StockService);
  private readonly router = inject(Router);
  private readonly notify = inject(NotificationService);
  private readonly destroyRef = inject(DestroyRef);

  readonly id = input<string>();

  protected readonly fieldError = fieldError;
  protected readonly saving = signal(false);
  protected readonly existing = signal<LoadState<Stock>>({
    loading: false,
    data: null,
    error: null,
  });

  protected readonly form = inject(FormBuilder).nonNullable.group({
    libelleStock: ['', [Validators.required, notBlank]],
    qte: [0, [Validators.required, Validators.min(0), integer]],
    qteMin: [0, [Validators.required, Validators.min(0), integer]],
  });

  ngOnInit(): void {
    this.load();
  }

  protected load(): void {
    const id = this.id();
    if (!id) {
      return;
    }
    loadState(this.api.get(Number(id)))
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((state) => {
        this.existing.set(state);
        if (state.data) {
          const { libelleStock, qte, qteMin } = state.data;
          this.form.setValue({ libelleStock, qte, qteMin });
        }
      });
  }

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const id = this.id();
    const payload = this.form.getRawValue();
    const request = id ? this.api.update(Number(id), payload) : this.api.create(payload);
    this.saving.set(true);
    request
      .pipe(
        finalize(() => this.saving.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (stock) => {
          this.notify.success(id ? 'Stock updated' : 'Stock created');
          void this.router.navigate(['/stocks', stock.idStock]);
        },
        error: (error: unknown) => this.notify.error(error),
      });
  }
}
