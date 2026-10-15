/** `CategorieProduitResponse` */
export interface CategorieProduit {
  idCategorieProduit: number;
  codeCategorie: string;
  libelleCategorie: string;
}

/** Writable fields of the `CategorieProduit` entity. */
export interface CategorieProduitPayload {
  codeCategorie: string;
  libelleCategorie: string;
}
