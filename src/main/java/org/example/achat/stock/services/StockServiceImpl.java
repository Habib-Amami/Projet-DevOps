package org.example.achat.stock.services;

import org.springframework.stereotype.Service;

import org.example.achat.stock.entities.Stock;
import org.example.achat.stock.exceptions.StockNotFoundException;
import org.example.achat.stock.repositories.StockRepository;

import java.util.List;

@Service
public class StockServiceImpl implements IStockService {

	private final StockRepository stockRepository;

	public StockServiceImpl(StockRepository stockRepository) {
		this.stockRepository = stockRepository;
	}


	@Override
	public List<Stock> retrieveAllStocks() {
		return stockRepository.findAll();
	}

	@Override
	public Stock addStock(Stock s) {
		return stockRepository.save(s);
	}

	@Override
	public void deleteStock(Long stockId) {
		if (!stockRepository.existsById(stockId)) {
			throw new StockNotFoundException(stockId);
		}
		stockRepository.deleteById(stockId);
	}

	@Override
	public Stock updateStock(Stock s) {
		Stock existingStock = stockRepository.findById(s.getIdStock())
				.orElseThrow(() -> new StockNotFoundException(s.getIdStock()));
		existingStock.setLibelleStock(s.getLibelleStock());
		existingStock.setQte(s.getQte());
		existingStock.setQteMin(s.getQteMin());
		return stockRepository.save(existingStock);
	}

	@Override
	public Stock retrieveStock(Long stockId) {
		return stockRepository.findById(stockId)
				.orElseThrow(() -> new StockNotFoundException(stockId));
	}

	@Override
	public String generateLowStockReport() {
		List<Stock> stocksEnRouge = stockRepository.findStocksBelowMinimumQuantity();
		StringBuilder message = new StringBuilder();
		for (Stock stock : stocksEnRouge) {
			message.append(": le stock ")
					.append(stock.getLibelleStock())
					.append(" a une quantité de ")
					.append(stock.getQte())
					.append(" inférieur à la quantité minimale a ne pas dépasser de ")
					.append(stock.getQteMin())
					.append(System.lineSeparator());
		}
		return message.toString();
	}

}