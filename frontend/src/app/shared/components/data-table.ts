import { formatDate, formatNumber } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  LOCALE_ID,
  computed,
  inject,
  input,
  output,
} from '@angular/core';
import { MatIconButton } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatTooltip } from '@angular/material/tooltip';

export type CellValue = string | number | boolean | null;

export interface TableColumn<T> {
  key: string;
  header: string;
  value: (row: T) => CellValue;
  type?: 'text' | 'amount' | 'date' | 'boolean';
}

export interface TableAction<T> {
  icon: string;
  label: string;
  warn?: boolean;
  visible?: (row: T) => boolean;
  run: (row: T) => void;
}

@Component({
  selector: 'app-data-table',
  imports: [MatTableModule, MatIconButton, MatIcon, MatTooltip],
  template: `
    <div class="table-container">
      <table mat-table [dataSource]="rows()">
        @for (column of columns(); track column.key) {
          <ng-container [matColumnDef]="column.key">
            <th mat-header-cell *matHeaderCellDef>{{ column.header }}</th>
            <td mat-cell *matCellDef="let row">{{ format(column, row) }}</td>
          </ng-container>
        }
        <ng-container matColumnDef="actions">
          <th mat-header-cell *matHeaderCellDef>
            <span class="cdk-visually-hidden">Actions</span>
          </th>
          <td mat-cell *matCellDef="let row" class="actions">
            @for (action of actions(); track action.label) {
              @if (!action.visible || action.visible(row)) {
                <button
                  mat-icon-button
                  type="button"
                  [class.warn]="action.warn"
                  [matTooltip]="action.label"
                  [attr.aria-label]="action.label"
                  (click)="$event.stopPropagation(); action.run(row)"
                >
                  <mat-icon>{{ action.icon }}</mat-icon>
                </button>
              }
            }
          </td>
        </ng-container>
        <tr mat-header-row *matHeaderRowDef="displayedColumns()"></tr>
        <tr
          mat-row
          *matRowDef="let row; columns: displayedColumns()"
          (click)="rowClick.emit(row)"
        ></tr>
      </table>
    </div>
  `,
  styles: `
    .table-container {
      overflow-x: auto;
    }
    table {
      min-width: 100%;
    }
    tr.mat-mdc-row {
      cursor: pointer;
    }
    tr.mat-mdc-row:hover {
      background: var(--mat-sys-surface-container-high);
    }
    .actions {
      text-align: right;
      white-space: nowrap;
    }
    .warn {
      color: var(--mat-sys-error);
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DataTable<T> {
  private readonly locale = inject(LOCALE_ID);

  readonly rows = input.required<readonly T[]>();
  readonly columns = input.required<readonly TableColumn<T>[]>();
  readonly actions = input<readonly TableAction<T>[]>([]);
  readonly rowClick = output<T>();

  protected readonly displayedColumns = computed(() => [
    ...this.columns().map((column) => column.key),
    'actions',
  ]);

  protected format(column: TableColumn<T>, row: T): string {
    const value = column.value(row);
    if (value === null || value === '') {
      return '—';
    }
    switch (column.type) {
      case 'amount':
        return formatNumber(Number(value), this.locale, '1.2-2');
      case 'date':
        return formatDate(String(value), 'mediumDate', this.locale);
      case 'boolean':
        return value ? 'Yes' : 'No';
      default:
        return String(value);
    }
  }
}
