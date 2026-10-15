import { ProduitEntity } from './produit.model';

/** `DetailFacture` entity as serialized by the backend. */
export interface DetailFacture {
  idDetailFacture: number;
  qteCommandee: number | null;
  prixTotalDetail: number;
  pourcentageRemise: number | null;
  montantRemise: number;
  produit: ProduitEntity | null;
}

/** `Facture` entity as serialized by the backend (`fournisseur` and `reglements` are `@JsonIgnore`d). */
export interface Facture {
  idFacture: number;
  montantRemise: number;
  montantFacture: number;
  dateCreationFacture: string | null;
  dateDerniereModificationFacture: string | null;
  archivee: boolean | null;
  detailsFacture: DetailFacture[] | null;
}

/** Writable fields of the `Facture` entity. Dates are `yyyy-MM-dd`. */
export interface FacturePayload {
  montantRemise: number;
  montantFacture: number;
  dateCreationFacture: string;
  archivee: boolean;
}
