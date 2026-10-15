import { Injectable } from '@angular/core';
import { CrudService } from '../api/crud.service';
import { SecteurActivite, SecteurActivitePayload } from '../models/secteur-activite.model';

@Injectable({ providedIn: 'root' })
export class SecteurActiviteService extends CrudService<SecteurActivite, SecteurActivitePayload> {
  constructor() {
    super('secteurs-activite');
  }
}
