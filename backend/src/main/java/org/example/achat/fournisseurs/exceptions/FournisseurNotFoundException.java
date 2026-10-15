package org.example.achat.fournisseurs.exceptions;

public class FournisseurNotFoundException extends RuntimeException {

	public FournisseurNotFoundException(Long fournisseurId) {
		super("Fournisseur not found with id: " + fournisseurId);
	}
}
