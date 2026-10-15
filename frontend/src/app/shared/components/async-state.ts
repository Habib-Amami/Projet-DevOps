import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { LoadState } from '../../core/api/load-state';
import { ErrorMessage } from './error-message';
import { Loader } from './loader';

/** Renders a loader or an error for a `LoadState`, and the projected content once loaded. */
@Component({
  selector: 'app-async-state',
  imports: [Loader, ErrorMessage],
  template: `
    @if (state().loading) {
      <app-loader />
    } @else if (state().error; as error) {
      <app-error-message [message]="error" (retry)="retry.emit()" />
    } @else {
      <ng-content />
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AsyncState {
  readonly state = input.required<LoadState<unknown>>();
  readonly retry = output();
}
