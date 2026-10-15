import { Injectable } from '@angular/core';
import { CrudService } from '../api/crud.service';
import { Stock, StockPayload } from '../models/stock.model';

@Injectable({ providedIn: 'root' })
export class StockService extends CrudService<Stock, StockPayload> {
  constructor() {
    super('stocks');
  }
}
