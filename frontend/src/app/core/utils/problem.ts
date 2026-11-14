import { HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { ApiProblemError, Problem } from '../models/project.model';

/**
 * Maps an HTTP error to {@link ApiProblemError}, preserving RFC 9457 Problem
 * Details bodies (both contracts share the same `Problem` schema). Non-HTTP
 * errors pass through unchanged.
 */
export function toApiProblem(error: unknown): Observable<never> {
  if (error instanceof HttpErrorResponse) {
    const body = error.error;
    if (body && typeof body === 'object' && typeof (body as Problem).title === 'string') {
      return throwError(() => new ApiProblemError(error.status, body as Problem));
    }
    return throwError(
      () =>
        new ApiProblemError(error.status, {
          type: 'about:blank',
          title: error.statusText || 'Request failed',
          status: error.status,
        }),
    );
  }
  return throwError(() => error);
}
