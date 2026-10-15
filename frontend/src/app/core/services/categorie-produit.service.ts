import { Injectable } from '@angular/core';
import { CrudService } from '../api/crud.service';
import { CategorieProduit, CategorieProduitPayload } from '../models/categorie-produit.model';

@Injectable({ providedIn: 'root' })
export class CategorieProduitService extends CrudService<
  CategorieProduit,
  CategorieProduitPayload
> {
  constructor() {
    super('categories-produits');
  }
}
