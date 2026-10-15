/** `StockResponse` */
export interface Stock {
  idStock: number;
  libelleStock: string;
  qte: number;
  qteMin: number;
}

/** Writable fields of the `Stock` entity. */
export interface StockPayload {
  libelleStock: string;
  qte: number;
  qteMin: number;
}
