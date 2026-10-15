package org.example.achat.produit.dto;

public record ProduitResponse(
		Long idProduit,
		String codeProduit,
		String libelleProduit,
		float prix,
		Long stockId,
		Long categorieProduitId) {
}
