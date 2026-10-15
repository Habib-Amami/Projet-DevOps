import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { CrudService } from '../api/crud.service';
import { Fournisseur, FournisseurPayload } from '../models/fournisseur.model';

@Injectable({ providedIn: 'root' })
export class FournisseurService extends CrudService<Fournisseur, FournisseurPayload> {
  constructor() {
    super('fournisseurs');
  }

  assignSecteurActivite(fournisseurId: number, secteurActiviteId: number): Observable<Fournisseur> {
    return this.http.put<Fournisseur>(
      `${this.baseUrl}/${fournisseurId}/secteurs-activite/${secteurActiviteId}`,
      null,
    );
  }
}
