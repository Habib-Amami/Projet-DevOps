package org.example.achat.produit.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.Date;

public record ProduitRequest(
		@NotBlank String codeProduit,
		@NotBlank String libelleProduit,
		@PositiveOrZero float prix,
		Date dateCreation,
		Date dateDerniereModification) {
}
