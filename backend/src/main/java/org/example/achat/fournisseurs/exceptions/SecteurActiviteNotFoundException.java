package org.example.achat.fournisseurs.exceptions;

public class SecteurActiviteNotFoundException extends RuntimeException {

	public SecteurActiviteNotFoundException(Long secteurActiviteId) {
		super("Secteur activite not found with id: " + secteurActiviteId);
	}
}
