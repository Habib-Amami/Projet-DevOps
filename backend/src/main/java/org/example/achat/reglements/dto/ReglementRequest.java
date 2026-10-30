package org.example.achat.reglements.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.Date;

public record ReglementRequest(
		@PositiveOrZero float montantPaye,
		@PositiveOrZero float montantRestant,
		@NotNull Boolean payee,
		@NotNull Date dateReglement,
		@Positive @NotNull(groups = Create.class) Long factureId) {

	/** The invoice is selected on creation and remains unchanged on update. */
	public interface Create {
	}
}
