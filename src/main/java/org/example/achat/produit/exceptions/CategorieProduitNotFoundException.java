package org.example.achat.produit.exceptions;

public class CategorieProduitNotFoundException extends RuntimeException {

	public CategorieProduitNotFoundException(Long categorieProduitId) {
		super("Categorie produit not found with id: " + categorieProduitId);
	}
}
