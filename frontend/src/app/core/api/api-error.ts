import { HttpErrorResponse } from '@angular/common/http';

/** Normalized error raised for every failed API call. */
export class ApiError extends Error {
  constructor(
    readonly status: number,
    message: string,
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

interface ErrorBody {
  detail?: string;
  message?: string;
  error?: string;
}

function isErrorBody(value: unknown): value is ErrorBody {
  return typeof value === 'object' && value !== null;
}

/** Extracts a readable message from a Spring `ProblemDetail` or default error body. */
export function toApiError(response: HttpErrorResponse): ApiError {
  if (response.status === 0) {
    return new ApiError(0, 'Cannot reach the server. Is the backend running?');
  }
  const body: unknown = response.error;
  const message = isErrorBody(body) ? (body.detail ?? body.message ?? body.error) : undefined;
  return new ApiError(response.status, message ?? `Request failed (HTTP ${response.status})`);
}

export function errorMessage(error: unknown): string {
  return error instanceof Error ? error.message : 'Unexpected error';
}
