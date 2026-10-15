import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { MatIcon } from '@angular/material/icon';

@Component({
  selector: 'app-empty-state',
  imports: [MatIcon],
  template: `
    <mat-icon>inbox</mat-icon>
    <p>{{ message() }}</p>
  `,
  styles: `
    :host {
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 32px;
      color: var(--mat-sys-on-surface-variant);
    }
    p {
      margin: 8px 0 0;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EmptyState {
  readonly message = input('Nothing to show yet.');
}
