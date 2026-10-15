export const CATEGORIES_FOURNISSEUR = ['ORDINAIRE', 'CONVENTIONNE'] as const;

/** `CategorieFournisseur` enum */
export type CategorieFournisseur = (typeof CATEGORIES_FOURNISSEUR)[number];

/** `FournisseurResponse` */
export interface Fournisseur {
  idFournisseur: number;
  code: string;
  libelle: string;
  categorieFournisseur: CategorieFournisseur;
  detailFournisseurId: number | null;
  secteurActiviteIds: number[];
}

/** Writable fields of the `DetailFournisseur` entity. Dates are `yyyy-MM-dd`. */
export interface DetailFournisseurPayload {
  email: string | null;
  dateDebutCollaboration: string | null;
  adresse: string;
  matricule: string;
}

/** Writable fields of the `Fournisseur` entity. Omitting `detailFournisseur` on update keeps the existing one. */
export interface FournisseurPayload {
  code: string;
  libelle: string;
  categorieFournisseur: CategorieFournisseur;
  detailFournisseur?: DetailFournisseurPayload;
}
