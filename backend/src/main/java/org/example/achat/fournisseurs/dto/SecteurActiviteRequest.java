package org.example.achat.fournisseurs.dto;

import jakarta.validation.constraints.NotBlank;

public record SecteurActiviteRequest(
		@NotBlank String codeSecteurActivite,
		@NotBlank String libelleSecteurActivite) {
}
