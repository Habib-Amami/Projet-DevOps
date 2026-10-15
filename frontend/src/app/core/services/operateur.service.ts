import { Injectable } from '@angular/core';
import { CrudService } from '../api/crud.service';
import { Operateur, OperateurPayload } from '../models/operateur.model';

@Injectable({ providedIn: 'root' })
export class OperateurService extends CrudService<Operateur, OperateurPayload> {
  constructor() {
    super('operateurs');
  }
}
