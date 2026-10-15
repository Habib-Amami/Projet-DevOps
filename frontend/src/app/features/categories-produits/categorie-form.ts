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
import { CategorieProduit } from '../../core/models/categorie-produit.model';
import { CategorieProduitService } from '../../core/services/categorie-produit.service';
import { NotificationService } from '../../core/services/notification.service';
import { AsyncState } from '../../shared/components/async-state';
import { PageHeader } from '../../shared/components/page-header';
import { fieldError } from '../../shared/forms/field-error';
import { notBlank } from '../../shared/forms/validators';

@Component({
  selector: 'app-categorie-form',
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
    <app-page-header
      [title]="id() ? 'Edit category' : 'New category'"
      backLink="/categories-produits"
    />
    <app-async-state [state]="existing()" (retry)="load()">
      <form class="form" [formGroup]="form" (ngSubmit)="submit()">
        <mat-form-field>
          <mat-label>Code</mat-label>
          <input matInput formControlName="codeCategorie" />
          <mat-error>{{ fieldError(form.controls.codeCategorie) }}</mat-error>
        </mat-form-field>
        <mat-form-field>
          <mat-label>Label</mat-label>
          <input matInput formControlName="libelleCategorie" />
          <mat-error>{{ fieldError(form.controls.libelleCategorie) }}</mat-error>
        </mat-form-field>
        <div class="form-actions">
          <button mat-flat-button type="submit" [disabled]="saving()">Save</button>
          <a mat-button routerLink="/categories-produits">Cancel</a>
        </div>
      </form>
    </app-async-state>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export default class CategorieForm implements OnInit {
  private readonly api = inject(CategorieProduitService);
  private readonly router = inject(Router);
  private readonly notify = inject(NotificationService);
  private readonly destroyRef = inject(DestroyRef);

  readonly id = input<string>();

  protected readonly fieldError = fieldError;
  protected readonly saving = signal(false);
  protected readonly existing = signal<LoadState<CategorieProduit>>({
    loading: false,
    data: null,
    error: null,
  });

  protected readonly form = inject(FormBuilder).nonNullable.group({
    codeCategorie: ['', [Validators.required, notBlank]],
    libelleCategorie: ['', [Validators.required, notBlank]],
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
          const { codeCategorie, libelleCategorie } = state.data;
          this.form.setValue({ codeCategorie, libelleCategorie });
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
        next: (categorie) => {
          this.notify.success(id ? 'Category updated' : 'Category created');
          void this.router.navigate(['/categories-produits', categorie.idCategorieProduit]);
        },
        error: (error: unknown) => this.notify.error(error),
      });
  }
}
