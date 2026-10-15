/** `SecteurActiviteResponse` */
export interface SecteurActivite {
  idSecteurActivite: number;
  codeSecteurActivite: string;
  libelleSecteurActivite: string;
}

/** Writable fields of the `SecteurActivite` entity. */
export interface SecteurActivitePayload {
  codeSecteurActivite: string;
  libelleSecteurActivite: string;
}
