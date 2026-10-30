
package org.example.achat.facture.dto;

public record DetailFactureResponse(

        Long idDetailFacture,

        Long idProduit,

        String codeProduit,

        String libelleProduit,

        Integer qteCommandee,

        float prixTotalDetail,

        Integer pourcentageRemise,

        float montantRemise

) {
}
