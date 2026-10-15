import { HttpClient } from '@angular/common/http';
import { inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

/** Typed wrapper for the backend's standard REST resources (`GET/POST /x`, `GET/PUT/DELETE /x/{id}`). */
export abstract class CrudService<T, P> {
  protected readonly http = inject(HttpClient);

  protected constructor(private readonly resourcePath: string) {}

  protected get baseUrl(): string {
    return `${environment.apiUrl}/${this.resourcePath}`;
  }

  list(): Observable<T[]> {
    return this.http.get<T[]>(this.baseUrl);
  }

  get(id: number): Observable<T> {
    return this.http.get<T>(`${this.baseUrl}/${id}`);
  }

  create(payload: P): Observable<T> {
    return this.http.post<T>(this.baseUrl, payload);
  }

  update(id: number, payload: P): Observable<T> {
    return this.http.put<T>(`${this.baseUrl}/${id}`, payload);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
