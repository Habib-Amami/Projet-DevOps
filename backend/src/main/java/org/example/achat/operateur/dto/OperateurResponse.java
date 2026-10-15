package org.example.achat.operateur.dto;

import java.util.Set;

public record OperateurResponse(
		Long idOperateur,
		String nom,
		String prenom,
		Set<Long> factureIds) {
}
