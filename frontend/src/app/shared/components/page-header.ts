import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { MatIconButton } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-page-header',
  imports: [MatIconButton, MatIcon, RouterLink],
  template: `
    @if (backLink(); as link) {
      <a mat-icon-button [routerLink]="link" aria-label="Back"><mat-icon>arrow_back</mat-icon></a>
    }
    <h1>{{ title() }}</h1>
    <div class="actions"><ng-content /></div>
  `,
  styles: `
    :host {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      gap: 8px;
      margin-bottom: 16px;
    }
    h1 {
      flex: 1;
      margin: 0;
      font: var(--mat-sys-headline-small);
    }
    .actions {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PageHeader {
  readonly title = input.required<string>();
  readonly backLink = input<string>();
}
