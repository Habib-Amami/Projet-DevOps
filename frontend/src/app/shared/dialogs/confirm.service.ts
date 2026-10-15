import { Injectable, inject } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { EMPTY, Observable, catchError, filter, map, switchMap } from 'rxjs';
import { NotificationService } from '../../core/services/notification.service';
import { ConfirmDialog, ConfirmDialogData } from './confirm-dialog';

@Injectable({ providedIn: 'root' })
export class ConfirmService {
  private readonly dialog = inject(MatDialog);
  private readonly notify = inject(NotificationService);

  /**
   * Asks for confirmation, then runs `request` and reports the outcome in a snackbar.
   * Emits once if the request succeeds; completes silently if cancelled or failed.
   */
  confirmAndRun(
    dialogData: ConfirmDialogData,
    request: () => Observable<unknown>,
    successMessage: string,
  ): Observable<void> {
    return this.dialog
      .open<ConfirmDialog, ConfirmDialogData, boolean>(ConfirmDialog, {
        data: dialogData,
        width: '400px',
      })
      .afterClosed()
      .pipe(
        filter((confirmed) => confirmed === true),
        switchMap(() =>
          request().pipe(
            map(() => this.notify.success(successMessage)),
            catchError((error: unknown) => {
              this.notify.error(error);
              return EMPTY;
            }),
          ),
        ),
      );
  }
}
