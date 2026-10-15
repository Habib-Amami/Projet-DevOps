import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { MatButton } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';

@Component({
  selector: 'app-error-message',
  imports: [MatButton, MatIcon],
  template: `
    <div class="message" role="alert">
      <mat-icon>error_outline</mat-icon>
      <span>{{ message() }}</span>
    </div>
    @if (retryable()) {
      <button mat-stroked-button type="button" (click)="retry.emit()">Retry</button>
    }
  `,
  styles: `
    :host {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      justify-content: space-between;
      gap: 12px;
      padding: 16px;
      border-radius: 8px;
      background: var(--mat-sys-error-container);
      color: var(--mat-sys-on-error-container);
    }
    .message {
      display: flex;
      align-items: center;
      gap: 8px;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ErrorMessage {
  readonly message = input.required<string>();
  readonly retryable = input(true);
  readonly retry = output();
}
