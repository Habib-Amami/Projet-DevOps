
package org.example.achat.facture.mapper;

import org.example.achat.facture.dto.FactureResponse;
import org.example.achat.facture.dto.DetailFactureResponse;
import org.example.achat.facture.entities.Facture;
import org.example.achat.facture.entities.DetailFacture;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@Component
public class FactureMapper {

    /**
     * Convertit une entité Facture en FactureResponse.
     */
    public FactureResponse toResponse(Facture facture) {

        if (facture == null) {
            return null;
        }

        Long idFournisseur = null;
        String codeFournisseur = null;
        String libelleFournisseur = null;

        if (facture.getFournisseur() != null) {
            idFournisseur = facture.getFournisseur().getIdFournisseur();
            codeFournisseur = facture.getFournisseur().getCode();
            libelleFournisseur = facture.getFournisseur().getLibelle();
        }

        return new FactureResponse(
                facture.getIdFacture(),
                facture.getMontantRemise(),
                facture.getMontantFacture(),
                facture.getDateCreationFacture(),
                facture.getDateDerniereModificationFacture(),
                facture.getArchivee(),
                idFournisseur,
                codeFournisseur,
                libelleFournisseur,
                toDetailResponses(facture.getDetailsFacture())
        );
    }

    /**
     * Convertit une liste de factures en liste de DTO.
     */
    public List<FactureResponse> toResponseList(List<Facture> factures) {

        if (factures == null) {
            return Collections.emptyList();
        }

        return factures.stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Convertit un DetailFacture en DetailFactureResponse.
     */
    public DetailFactureResponse toDetailResponse(DetailFacture detail) {

        if (detail == null) {
            return null;
        }

        Long idProduit = null;
        String codeProduit = null;
        String libelleProduit = null;

        if (detail.getProduit() != null) {
            idProduit = detail.getProduit().getIdProduit();
            codeProduit = detail.getProduit().getCodeProduit();
            libelleProduit = detail.getProduit().getLibelleProduit();
        }

        return new DetailFactureResponse(
                detail.getIdDetailFacture(),
                idProduit,
                codeProduit,
                libelleProduit,
                detail.getQteCommandee(),
                detail.getPrixTotalDetail(),
                detail.getPourcentageRemise(),
                detail.getMontantRemise()
        );
    }

    /**
     * Convertit un ensemble de détails en liste de DTO.
     */
    public List<DetailFactureResponse> toDetailResponses(
            Set<DetailFacture> details) {

        if (details == null) {
            return Collections.emptyList();
        }

        return details.stream()
                .map(this::toDetailResponse)
                .toList();
    }
}
