package org.example.achat.reglements.exceptions;

public class ReglementNotFoundException extends RuntimeException {

	public ReglementNotFoundException(Long reglementId) {
		super("Reglement not found with id: " + reglementId);
	}
}
