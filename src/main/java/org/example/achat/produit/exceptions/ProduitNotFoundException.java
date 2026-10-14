package org.example.achat.produit.exceptions;

public class ProduitNotFoundException extends RuntimeException {

	public ProduitNotFoundException(Long produitId) {
		super("Produit not found with id: " + produitId);
	}
}
