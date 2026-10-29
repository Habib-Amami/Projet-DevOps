package org.example.achat.fournisseurs.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.example.achat.fournisseurs.entities.CategorieFournisseur;

public record FournisseurRequest(
		@NotBlank String code,
		@NotBlank String libelle,
		@NotNull CategorieFournisseur categorieFournisseur,
		@Valid @NotNull(groups = Create.class) DetailFournisseurRequest detailFournisseur) {

	/** Supplier details are required on creation and may be omitted on update. */
	public interface Create {
	}
}
