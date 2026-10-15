import { Injectable, inject } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { errorMessage } from '../api/api-error';

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly snackBar = inject(MatSnackBar);

  success(message: string): void {
    this.snackBar.open(message, 'OK', { duration: 3000 });
  }

  error(error: unknown): void {
    this.snackBar.open(errorMessage(error), 'Dismiss', {
      duration: 6000,
      panelClass: 'snackbar-error',
    });
  }
}
