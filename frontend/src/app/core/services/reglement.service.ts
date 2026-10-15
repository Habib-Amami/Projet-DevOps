import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Reglement } from '../models/reglement.model';

/**
 * `POST`/`PUT /reglements` are not exposed: the backend requires a `facture` that its JSON mapping ignores,
 * so those calls always fail validation.
 */
@Injectable({ providedIn: 'root' })
export class ReglementService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/reglements`;

  list(): Observable<Reglement[]> {
    return this.http.get<Reglement[]>(this.baseUrl);
  }

  get(id: number): Observable<Reglement> {
    return this.http.get<Reglement>(`${this.baseUrl}/${id}`);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  listByFacture(factureId: number): Observable<Reglement[]> {
    return this.http.get<Reglement[]>(`${this.baseUrl}/facture/${factureId}`);
  }

  /** Dates are `yyyy-MM-dd`. */
  revenue(startDate: string, endDate: string): Observable<number> {
    return this.http.get<number>(`${this.baseUrl}/revenue`, { params: { startDate, endDate } });
  }
}
