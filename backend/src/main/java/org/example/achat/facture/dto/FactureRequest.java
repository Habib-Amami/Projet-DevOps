package org.example.achat.facture.dto;

import jakarta.validation.constraints.PositiveOrZero;

import java.util.Date;

public record FactureRequest(
		@PositiveOrZero float montantRemise,
		@PositiveOrZero float montantFacture,
		Date dateCreationFacture,
		Date dateDerniereModificationFacture,
		Boolean archivee) {
}
