import { HttpErrorResponse } from '@angular/common/http';
import { toApiError } from './api-error';

describe('toApiError', () => {
  it('uses the ProblemDetail detail', () => {
    const response = new HttpErrorResponse({
      status: 404,
      error: { title: 'Not Found', detail: 'Stock not found with id: 9' },
    });
    expect(toApiError(response).message).toBe('Stock not found with id: 9');
  });

  it('reports an unreachable server', () => {
    const error = toApiError(new HttpErrorResponse({ status: 0 }));
    expect(error.status).toBe(0);
    expect(error.message).toContain('Cannot reach the server');
  });

  it('falls back to the HTTP status', () => {
    const response = new HttpErrorResponse({ status: 500, error: null });
    expect(toApiError(response).message).toBe('Request failed (HTTP 500)');
  });
});
