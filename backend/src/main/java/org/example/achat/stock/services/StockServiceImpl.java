package org.example.achat.stock.services;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import org.example.achat.stock.entities.Stock;
import org.example.achat.stock.exceptions.StockNotFoundException;
import org.example.achat.stock.repositories.StockRepository;

import java.util.List;

@Service
@Slf4j
public class StockServiceImpl implements IStockService {

	private final StockRepository stockRepository;

	public StockServiceImpl(StockRepository stockRepository) {
		this.stockRepository = stockRepository;
	}


	@Override
	public List<Stock> retrieveAllStocks() {
		List<Stock> stocks = stockRepository.findAll();
		log.info("Retrieved {} stocks", stocks.size());
		return stocks;
	}

	@Override
	public Stock addStock(Stock s) {
		log.debug("Adding stock '{}'", s.getLibelleStock());
		Stock savedStock = stockRepository.save(s);
		log.info("Stock '{}' added with id {}", savedStock.getLibelleStock(), savedStock.getIdStock());
		return savedStock;
	}

	@Override
	public void deleteStock(Long stockId) {
		if (!stockRepository.existsById(stockId)) {
			log.warn("Cannot delete stock {} because it does not exist", stockId);
			throw new StockNotFoundException(stockId);
		}
		stockRepository.deleteById(stockId);
		log.info("Deleted stock {}", stockId);
	}

	@Override
	public Stock updateStock(Stock s) {
		Stock existingStock = stockRepository.findById(s.getIdStock())
				.orElseThrow(() -> {
					log.warn("Cannot update stock {} because it does not exist", s.getIdStock());
					return new StockNotFoundException(s.getIdStock());
				});
		existingStock.setLibelleStock(s.getLibelleStock());
		existingStock.setQte(s.getQte());
		existingStock.setQteMin(s.getQteMin());
		Stock updatedStock = stockRepository.save(existingStock);
		log.info("Updated stock {}", updatedStock.getIdStock());
		return updatedStock;
	}

	@Override
	public Stock retrieveStock(Long stockId) {
		return stockRepository.findById(stockId)
				.orElseThrow(() -> {
					log.warn("Stock {} was not found", stockId);
					return new StockNotFoundException(stockId);
				});
	}

	@Override
	public String generateLowStockReport() {
		List<Stock> stocksEnRouge = stockRepository.findStocksBelowMinimumQuantity();
		log.debug("Generating low-stock report for {} stocks", stocksEnRouge.size());
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
		log.info("Generated low-stock report with {} entries", stocksEnRouge.size());
		return message.toString();
	}

}