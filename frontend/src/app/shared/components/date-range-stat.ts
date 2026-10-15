import { DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, input } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButton } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { Observable, Subject, switchMap } from 'rxjs';
import { loadState } from '../../core/api/load-state';
import { toIsoDate } from '../utils/date';
import { Loader } from './loader';

export type DateRangeQuery = (startDate: string, endDate: string) => Observable<number>;

/** Card that runs a numeric query over a date range (revenue, recovery rate). */
@Component({
  selector: 'app-date-range-stat',
  imports: [
    ReactiveFormsModule,
    DecimalPipe,
    MatCardModule,
    MatButton,
    MatDatepickerModule,
    MatFormFieldModule,
    Loader,
  ],
  template: `
    <mat-card appearance="outlined">
      <mat-card-header>
        <mat-card-title>{{ title() }}</mat-card-title>
      </mat-card-header>
      <mat-card-content>
        <form [formGroup]="form" (ngSubmit)="submit()" class="range-form">
          <mat-form-field subscriptSizing="dynamic">
            <mat-label>Period</mat-label>
            <mat-date-range-input [rangePicker]="picker">
              <input matStartDate formControlName="start" placeholder="Start date" />
              <input matEndDate formControlName="end" placeholder="End date" />
            </mat-date-range-input>
            <mat-datepicker-toggle matIconSuffix [for]="picker" />
            <mat-date-range-picker #picker />
          </mat-form-field>
          <button mat-flat-button type="submit" [disabled]="form.invalid">Compute</button>
        </form>
        @if (result(); as state) {
          @if (state.loading) {
            <app-loader />
          } @else if (state.error) {
            <p class="error">{{ state.error }}</p>
          } @else {
            <p class="value">{{ state.data | number: '1.0-2' }}{{ suffix() }}</p>
          }
        }
      </mat-card-content>
    </mat-card>
  `,
  styles: `
    .range-form {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      gap: 12px;
      margin-top: 8px;
    }
    .value {
      font: var(--mat-sys-headline-medium);
      margin: 16px 0 0;
    }
    .error {
      color: var(--mat-sys-error);
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DateRangeStat {
  readonly title = input.required<string>();
  readonly query = input.required<DateRangeQuery>();
  readonly suffix = input('');

  private readonly submissions = new Subject<{ start: Date; end: Date }>();

  protected readonly form = inject(FormBuilder).group({
    start: [null as Date | null, Validators.required],
    end: [null as Date | null, Validators.required],
  });

  protected readonly result = toSignal(
    this.submissions.pipe(
      switchMap(({ start, end }) => loadState(this.query()(toIsoDate(start), toIsoDate(end)))),
    ),
    { initialValue: null },
  );

  protected submit(): void {
    const { start, end } = this.form.getRawValue();
    if (start && end) {
      this.submissions.next({ start, end });
    }
  }
}
