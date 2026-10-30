
package org.example.achat.facture.dto;

import java.util.Date;
import java.util.List;

public record FactureResponse(

        Long idFacture,

        float montantRemise,

        float montantFacture,

        Date dateCreationFacture,

        Date dateDerniereModificationFacture,

        Boolean archivee,

        Long idFournisseur,

        String codeFournisseur,

        String libelleFournisseur,

        List<DetailFactureResponse> detailsFacture

) {
}
