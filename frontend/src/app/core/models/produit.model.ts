/** `ProduitResponse` */
export interface Produit {
  idProduit: number;
  codeProduit: string;
  libelleProduit: string;
  prix: number;
  stockId: number | null;
  categorieProduitId: number | null;
}

/**
 * Writable fields of the `Produit` entity. `stock` and `categorieProduit` are `@JsonIgnore`d,
 * so the stock is assigned through `PUT /produits/{id}/stock/{stockId}` and the category cannot be set.
 */
export interface ProduitPayload {
  codeProduit: string;
  libelleProduit: string;
  prix: number;
}

/** `Produit` entity as embedded in a `DetailFacture`. */
export interface ProduitEntity {
  idProduit: number;
  codeProduit: string;
  libelleProduit: string;
  prix: number;
  dateCreation: string | null;
  dateDerniereModification: string | null;
}
