import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { CrudService } from '../api/crud.service';
import { Produit, ProduitPayload } from '../models/produit.model';

@Injectable({ providedIn: 'root' })
export class ProduitService extends CrudService<Produit, ProduitPayload> {
  constructor() {
    super('produits');
  }

  assignStock(produitId: number, stockId: number): Observable<Produit> {
    return this.http.put<Produit>(`${this.baseUrl}/${produitId}/stock/${stockId}`, null);
  }
}
