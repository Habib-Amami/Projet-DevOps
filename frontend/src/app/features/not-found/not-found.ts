import { ChangeDetectionStrategy, Component } from '@angular/core';
import { MatButton } from '@angular/material/button';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-not-found',
  imports: [MatButton, RouterLink],
  template: `
    <h1>404</h1>
    <p>This page does not exist.</p>
    <a mat-flat-button routerLink="/">Back to home</a>
  `,
  styles: `
    :host {
      display: block;
      text-align: center;
      padding: 48px 16px;
    }
    h1 {
      font: var(--mat-sys-display-medium);
      margin: 0;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export default class NotFound {}
