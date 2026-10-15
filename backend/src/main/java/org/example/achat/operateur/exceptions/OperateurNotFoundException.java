package org.example.achat.operateur.exceptions;

public class OperateurNotFoundException extends RuntimeException {

	public OperateurNotFoundException(Long operateurId) {
		super("Operateur not found with id: " + operateurId);
	}
}
