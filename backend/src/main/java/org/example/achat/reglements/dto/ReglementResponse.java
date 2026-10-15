package org.example.achat.reglements.dto;

import java.util.Date;

public record ReglementResponse(
		Long idReglement,
		float montantPaye,
		float montantRestant,
		Boolean payee,
		Date dateReglement,
		Long factureId) {
}
