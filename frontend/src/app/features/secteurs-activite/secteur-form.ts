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
import { SecteurActivite } from '../../core/models/secteur-activite.model';
import { SecteurActiviteService } from '../../core/services/secteur-activite.service';
import { NotificationService } from '../../core/services/notification.service';
import { AsyncState } from '../../shared/components/async-state';
import { PageHeader } from '../../shared/components/page-header';
import { fieldError } from '../../shared/forms/field-error';
import { notBlank } from '../../shared/forms/validators';

@Component({
  selector: 'app-secteur-form',
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
    <app-page-header [title]="id() ? 'Edit sector' : 'New sector'" backLink="/secteurs-activite" />
    <app-async-state [state]="existing()" (retry)="load()">
      <form class="form" [formGroup]="form" (ngSubmit)="submit()">
        <mat-form-field>
          <mat-label>Code</mat-label>
          <input matInput formControlName="codeSecteurActivite" />
          <mat-error>{{ fieldError(form.controls.codeSecteurActivite) }}</mat-error>
        </mat-form-field>
        <mat-form-field>
          <mat-label>Label</mat-label>
          <input matInput formControlName="libelleSecteurActivite" />
          <mat-error>{{ fieldError(form.controls.libelleSecteurActivite) }}</mat-error>
        </mat-form-field>
        <div class="form-actions">
          <button mat-flat-button type="submit" [disabled]="saving()">Save</button>
          <a mat-button routerLink="/secteurs-activite">Cancel</a>
        </div>
      </form>
    </app-async-state>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export default class SecteurForm implements OnInit {
  private readonly api = inject(SecteurActiviteService);
  private readonly router = inject(Router);
  private readonly notify = inject(NotificationService);
  private readonly destroyRef = inject(DestroyRef);

  readonly id = input<string>();

  protected readonly fieldError = fieldError;
  protected readonly saving = signal(false);
  protected readonly existing = signal<LoadState<SecteurActivite>>({
    loading: false,
    data: null,
    error: null,
  });

  protected readonly form = inject(FormBuilder).nonNullable.group({
    codeSecteurActivite: ['', [Validators.required, notBlank]],
    libelleSecteurActivite: ['', [Validators.required, notBlank]],
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
          const { codeSecteurActivite, libelleSecteurActivite } = state.data;
          this.form.setValue({ codeSecteurActivite, libelleSecteurActivite });
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
        next: (secteur) => {
          this.notify.success(id ? 'Sector updated' : 'Sector created');
          void this.router.navigate(['/secteurs-activite', secteur.idSecteurActivite]);
        },
        error: (error: unknown) => this.notify.error(error),
      });
  }
}
