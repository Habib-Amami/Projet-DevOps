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
import { MatCheckbox } from '@angular/material/checkbox';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInput } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { LoadState, loadState } from '../../core/api/load-state';
import {
  CATEGORIES_FOURNISSEUR,
  CategorieFournisseur,
  Fournisseur,
  FournisseurPayload,
} from '../../core/models/fournisseur.model';
import { FournisseurService } from '../../core/services/fournisseur.service';
import { NotificationService } from '../../core/services/notification.service';
import { AsyncState } from '../../shared/components/async-state';
import { PageHeader } from '../../shared/components/page-header';
import { fieldError } from '../../shared/forms/field-error';
import { notBlank } from '../../shared/forms/validators';
import { toIsoDate } from '../../shared/utils/date';

@Component({
  selector: 'app-fournisseur-form',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatButton,
    MatCheckbox,
    MatDatepickerModule,
    MatFormFieldModule,
    MatInput,
    MatSelectModule,
    PageHeader,
    AsyncState,
  ],
  templateUrl: './fournisseur-form.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export default class FournisseurForm implements OnInit {
  private readonly api = inject(FournisseurService);
  private readonly router = inject(Router);
  private readonly notify = inject(NotificationService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly fb = inject(FormBuilder).nonNullable;

  readonly id = input<string>();

  protected readonly categories = CATEGORIES_FOURNISSEUR;
  protected readonly fieldError = fieldError;
  protected readonly saving = signal(false);
  /** On update the backend keeps the existing details unless new ones are sent. */
  protected readonly withDetail = signal(true);
  protected readonly existing = signal<LoadState<Fournisseur>>({
    loading: false,
    data: null,
    error: null,
  });

  protected readonly form = this.fb.group({
    code: ['', [Validators.required, notBlank]],
    libelle: ['', [Validators.required, notBlank]],
    categorieFournisseur: ['ORDINAIRE' as CategorieFournisseur, Validators.required],
    detail: this.fb.group({
      adresse: ['', [Validators.required, notBlank]],
      matricule: ['', [Validators.required, notBlank]],
      email: ['', Validators.email],
      dateDebutCollaboration: [null as Date | null],
    }),
  });

  ngOnInit(): void {
    if (this.id()) {
      this.toggleDetail(false);
    }
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
          const { code, libelle, categorieFournisseur } = state.data;
          this.form.patchValue({ code, libelle, categorieFournisseur });
        }
      });
  }

  protected toggleDetail(enabled: boolean): void {
    this.withDetail.set(enabled);
    const detail = this.form.controls.detail;
    if (enabled) {
      detail.enable();
    } else {
      detail.disable();
    }
  }

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const id = this.id();
    const payload = this.toPayload();
    const request = id ? this.api.update(Number(id), payload) : this.api.create(payload);
    this.saving.set(true);
    request
      .pipe(
        finalize(() => this.saving.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (fournisseur) => {
          this.notify.success(id ? 'Supplier updated' : 'Supplier created');
          void this.router.navigate(['/fournisseurs', fournisseur.idFournisseur]);
        },
        error: (error: unknown) => this.notify.error(error),
      });
  }

  private toPayload(): FournisseurPayload {
    const { detail, ...fournisseur } = this.form.getRawValue();
    if (!this.withDetail()) {
      return fournisseur;
    }
    return {
      ...fournisseur,
      detailFournisseur: {
        adresse: detail.adresse,
        matricule: detail.matricule,
        email: detail.email || null,
        dateDebutCollaboration: detail.dateDebutCollaboration
          ? toIsoDate(detail.dateDebutCollaboration)
          : null,
      },
    };
  }
}
