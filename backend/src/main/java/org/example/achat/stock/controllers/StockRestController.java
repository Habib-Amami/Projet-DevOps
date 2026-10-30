package org.example.achat.stock.controllers;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.achat.stock.dto.StockResponse;
import org.example.achat.stock.dto.StockRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import lombok.extern.slf4j.Slf4j;
import org.example.achat.stock.entities.Stock;
import org.example.achat.stock.services.IStockService;

import java.util.List;

@RestController
@Slf4j
@RequestMapping("/stocks")
@Tag(name = "Stocks", description = "Stock inventory management")
public class StockRestController {

	private final IStockService stockService;

	public StockRestController(IStockService stockService) {
		this.stockService = stockService;
	}

	@GetMapping
	@Operation(summary = "List stocks", description = "Returns all stocks.")
	public List<StockResponse> getStocks() {
		List<StockResponse> responses = stockService.retrieveAllStocks().stream()
				.map(this::toResponse)
				.toList();
		log.info("Returning {} stocks", responses.size());
		return responses;
	}

	@GetMapping("/{stockId}")
	@Operation(summary = "Get a stock", description = "Returns a stock by its identifier.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Stock found"),
			@ApiResponse(responseCode = "404", description = "Stock not found")
	})
	public StockResponse retrieveStock(@PathVariable Long stockId) {
		log.debug("Retrieving stock {} through REST API", stockId);
		return toResponse(stockService.retrieveStock(stockId));
	}

	@PostMapping
	@Operation(summary = "Create a stock", description = "Creates a stock with a label and quantity thresholds.")
	@ApiResponse(responseCode = "201", description = "Stock created")
	public ResponseEntity<StockResponse> addStock(@Valid @RequestBody StockRequest request) {
		Stock stock = toEntity(request);
		log.info("Creating stock '{}' through REST API", stock.getLibelleStock());
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(toResponse(stockService.addStock(stock)));
	}

	@DeleteMapping("/{stockId}")
	@Operation(summary = "Delete a stock", description = "Deletes a stock by its identifier.")
	@ApiResponses({
			@ApiResponse(responseCode = "204", description = "Stock deleted"),
			@ApiResponse(responseCode = "404", description = "Stock not found")
	})
	public ResponseEntity<Void> removeStock(@PathVariable Long stockId) {
		log.info("Deleting stock {} through REST API", stockId);
		stockService.deleteStock(stockId);
		return ResponseEntity.noContent().build();
	}

	@PutMapping("/{stockId}")
	@Operation(summary = "Update a stock", description = "Updates a stock by its identifier.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Stock updated"),
			@ApiResponse(responseCode = "404", description = "Stock not found")
	})
	public ResponseEntity<StockResponse> modifyStock(
			@PathVariable Long stockId,
			@Valid @RequestBody StockRequest request) {
		log.info("Updating stock {} through REST API", stockId);
		Stock stock = toEntity(request);
		stock.setIdStock(stockId);
		return ResponseEntity.ok(toResponse(stockService.updateStock(stock)));
	}

	private Stock toEntity(StockRequest request) {
		Stock stock = new Stock();
		stock.setLibelleStock(request.libelleStock());
		stock.setQte(request.qte());
		stock.setQteMin(request.qteMin());
		return stock;
	}

	private StockResponse toResponse(Stock stock) {
		return new StockResponse(
				stock.getIdStock(),
				stock.getLibelleStock(),
				stock.getQte(),
				stock.getQteMin());
	}
}
