/** `ReglementResponse` */
export interface Reglement {
  idReglement: number;
  montantPaye: number;
  montantRestant: number;
  payee: boolean;
  dateReglement: string;
  factureId: number | null;
}
