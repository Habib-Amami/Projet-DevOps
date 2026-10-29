package org.example.achat.produit.dto;

import jakarta.validation.constraints.NotBlank;

public record CategorieProduitRequest(
		@NotBlank String codeCategorie,
		@NotBlank String libelleCategorie) {
}
