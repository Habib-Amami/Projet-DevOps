package org.example.achat.facture.dto;



import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record FactureRequest(

        @NotNull(message = "Le fournisseur est obligatoire")
        @Positive(message = "L'identifiant du fournisseur doit être positif")
        Long idFournisseur,

        @NotEmpty(message = "Une facture doit contenir au moins un détail")
        List<@Valid DetailFactureRequest> detailsFacture

) {
}
