
package org.example.achat.facture.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record DetailFactureRequest(

        @NotNull(message = "L'identifiant du produit est obligatoire")
        @Positive(message = "L'identifiant du produit doit être positif")
        Long idProduit,

        @NotNull(message = "La quantité est obligatoire")
        @Positive(message = "La quantité doit être positive")
        Integer qteCommandee,

        @NotNull(message = "Le pourcentage de remise est obligatoire")
        @Min(value = 0, message = "La remise minimale est 0%")
        @Max(value = 100, message = "La remise maximale est 100%")
        Integer pourcentageRemise

) {
}
