/** `OperateurResponse` (the password is never returned). */
export interface Operateur {
  idOperateur: number;
  nom: string;
  prenom: string;
  factureIds: number[];
}

/** Writable fields of the `Operateur` entity. `password` is required on create and update. */
export interface OperateurPayload {
  nom: string;
  prenom: string;
  password: string;
}
