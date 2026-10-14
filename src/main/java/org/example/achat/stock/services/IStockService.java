package org.example.achat.stock.services;

import org.example.achat.stock.entities.Stock;

import java.util.List;

public interface IStockService {

	/** Returns all stocks. */
	List<Stock> retrieveAllStocks();

	/** Creates a stock. */
	Stock addStock(Stock s);

	/** Deletes a stock by its identifier. */
	void deleteStock(Long id);

	/** Updates an existing stock. */
	Stock updateStock(Stock u);

	/** Returns a stock by its identifier. */
	Stock retrieveStock(Long id);

	/** Builds a report for stocks below their minimum quantity. */
	String generateLowStockReport();
}
