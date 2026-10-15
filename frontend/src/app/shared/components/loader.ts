import { ChangeDetectionStrategy, Component } from '@angular/core';
import { MatProgressSpinner } from '@angular/material/progress-spinner';

@Component({
  selector: 'app-loader',
  imports: [MatProgressSpinner],
  template: `<mat-spinner diameter="40" aria-label="Loading" />`,
  styles: `
    :host {
      display: flex;
      justify-content: center;
      padding: 32px;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Loader {}
