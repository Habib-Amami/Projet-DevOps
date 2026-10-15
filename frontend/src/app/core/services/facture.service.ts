import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiError } from '../api/api-error';
import { Facture, FacturePayload } from '../models/facture.model';

/** Invoices cannot be edited or deleted: the backend only allows cancelling (archiving) them. */
@Injectable({ providedIn: 'root' })
export class FactureService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/facture`;

  list(): Observable<Facture[]> {
    return this.http.get<Facture[]>(`${this.baseUrl}/retrieve-all-factures`);
  }

  /** The backend answers `200` with an empty body for unknown ids. */
  get(id: number): Observable<Facture> {
    return this.http.get<Facture | null>(`${this.baseUrl}/retrieve-facture/${id}`).pipe(
      map((facture) => {
        if (!facture) {
          throw new ApiError(404, `Facture not found with id: ${id}`);
        }
        return facture;
      }),
    );
  }

  create(payload: FacturePayload): Observable<Facture> {
    return this.http.post<Facture>(`${this.baseUrl}/add-facture`, payload);
  }

  cancel(id: number): Observable<void> {
    return this.http.put<void>(`${this.baseUrl}/cancel-facture/${id}`, null);
  }

  listByFournisseur(fournisseurId: number): Observable<Facture[]> {
    return this.http.get<Facture[]>(`${this.baseUrl}/getFactureByFournisseur/${fournisseurId}`);
  }

  assignOperateur(operateurId: number, factureId: number): Observable<void> {
    return this.http.put<void>(
      `${this.baseUrl}/assignOperateurToFacture/${operateurId}/${factureId}`,
      null,
    );
  }

  /** Percentage of invoiced amounts recovered between two `yyyy-MM-dd` dates (the backend returns 0 on error). */
  recoveryRate(startDate: string, endDate: string): Observable<number> {
    return this.http.get<number>(`${this.baseUrl}/pourcentageRecouvrement/${startDate}/${endDate}`);
  }
}
