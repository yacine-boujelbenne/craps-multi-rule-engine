import { Injectable, inject } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';

export type NotificationKind = 'success' | 'error' | 'info';

/** Thin wrapper over MatSnackBar with consistent styling and durations. */
@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly snackBar = inject(MatSnackBar);

  success(message: string): void {
    this.open(message, 'success');
  }

  error(message: string): void {
    this.open(message, 'error');
  }

  info(message: string): void {
    this.open(message, 'info');
  }

  private open(message: string, kind: NotificationKind): void {
    this.snackBar.open(message, 'Dismiss', {
      duration: kind === 'error' ? 7000 : 4500,
      horizontalPosition: 'end',
      verticalPosition: 'bottom',
      panelClass: [`app-toast`, `app-toast--${kind}`],
    });
  }
}
