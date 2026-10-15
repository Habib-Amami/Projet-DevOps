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
import { Operateur } from '../../core/models/operateur.model';
import { NotificationService } from '../../core/services/notification.service';
import { OperateurService } from '../../core/services/operateur.service';
import { AsyncState } from '../../shared/components/async-state';
import { PageHeader } from '../../shared/components/page-header';
import { fieldError } from '../../shared/forms/field-error';
import { notBlank } from '../../shared/forms/validators';

@Component({
  selector: 'app-operateur-form',
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
    <app-page-header [title]="id() ? 'Edit operator' : 'New operator'" backLink="/operateurs" />
    <app-async-state [state]="existing()" (retry)="load()">
      <form class="form" [formGroup]="form" (ngSubmit)="submit()">
        <div class="form-row">
          <mat-form-field>
            <mat-label>First name</mat-label>
            <input matInput formControlName="prenom" />
            <mat-error>{{ fieldError(form.controls.prenom) }}</mat-error>
          </mat-form-field>
          <mat-form-field>
            <mat-label>Last name</mat-label>
            <input matInput formControlName="nom" />
            <mat-error>{{ fieldError(form.controls.nom) }}</mat-error>
          </mat-form-field>
        </div>
        <mat-form-field>
          <mat-label>Password</mat-label>
          <input matInput type="password" autocomplete="new-password" formControlName="password" />
          @if (id()) {
            <mat-hint>The API requires the password on every update.</mat-hint>
          }
          <mat-error>{{ fieldError(form.controls.password) }}</mat-error>
        </mat-form-field>
        <div class="form-actions">
          <button mat-flat-button type="submit" [disabled]="saving()">Save</button>
          <a mat-button routerLink="/operateurs">Cancel</a>
        </div>
      </form>
    </app-async-state>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export default class OperateurForm implements OnInit {
  private readonly api = inject(OperateurService);
  private readonly router = inject(Router);
  private readonly notify = inject(NotificationService);
  private readonly destroyRef = inject(DestroyRef);

  readonly id = input<string>();

  protected readonly fieldError = fieldError;
  protected readonly saving = signal(false);
  protected readonly existing = signal<LoadState<Operateur>>({
    loading: false,
    data: null,
    error: null,
  });

  protected readonly form = inject(FormBuilder).nonNullable.group({
    nom: ['', [Validators.required, notBlank]],
    prenom: ['', [Validators.required, notBlank]],
    password: ['', [Validators.required, notBlank]],
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
          const { nom, prenom } = state.data;
          this.form.patchValue({ nom, prenom });
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
        next: (operateur) => {
          this.notify.success(id ? 'Operator updated' : 'Operator created');
          void this.router.navigate(['/operateurs', operateur.idOperateur]);
        },
        error: (error: unknown) => this.notify.error(error),
      });
  }
}
