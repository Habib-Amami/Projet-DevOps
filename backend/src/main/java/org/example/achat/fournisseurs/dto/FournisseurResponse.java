package org.example.achat.fournisseurs.dto;

import org.example.achat.fournisseurs.entities.CategorieFournisseur;

import java.util.Set;

public record FournisseurResponse(
		Long idFournisseur,
		String code,
		String libelle,
		CategorieFournisseur categorieFournisseur,
		Long detailFournisseurId,
		Set<Long> secteurActiviteIds) {
}
