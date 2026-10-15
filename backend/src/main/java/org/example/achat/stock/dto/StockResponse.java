package org.example.achat.stock.dto;

public record StockResponse(
		Long idStock,
		String libelleStock,
		Integer qte,
		Integer qteMin
) {
	/**
	 * API representation of a stock that intentionally excludes its products.
	 */
}
