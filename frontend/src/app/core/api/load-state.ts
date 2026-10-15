import { Observable, catchError, map, of, startWith } from 'rxjs';
import { errorMessage } from './api-error';

export interface LoadState<T> {
  readonly loading: boolean;
  readonly data: T | null;
  readonly error: string | null;
}

export const LOADING: LoadState<never> = { loading: true, data: null, error: null };

/** Wraps a request into a stream of loading / success / error states. */
export function loadState<T>(source: Observable<T>): Observable<LoadState<T>> {
  return source.pipe(
    map((data): LoadState<T> => ({ loading: false, data, error: null })),
    catchError((error: unknown) => of({ loading: false, data: null, error: errorMessage(error) })),
    startWith(LOADING),
  );
}
