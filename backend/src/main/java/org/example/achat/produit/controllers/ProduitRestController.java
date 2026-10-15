package org.example.achat.produit.controllers;

import jakarta.validation.Valid;
import org.example.achat.produit.dto.ProduitResponse;
import org.example.achat.produit.entities.Produit;
import org.example.achat.produit.services.IProduitService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
@CrossOrigin("*")
@RequestMapping("/produits")
public class ProduitRestController {

	private final IProduitService produitService;

	public ProduitRestController(IProduitService produitService) {
		this.produitService = produitService;
	}

	@GetMapping
	public List<ProduitResponse> getProduits() {
		List<ProduitResponse> responses = produitService.retrieveAllProduits().stream()
				.map(this::toResponse)
				.toList();
		log.info("Returning {} products", responses.size());
		return responses;
	}

	@GetMapping("/{produitId}")
	public ProduitResponse retrieveProduit(@PathVariable Long produitId) {
		log.debug("Retrieving product {} through REST API", produitId);
		return toResponse(produitService.retrieveProduit(produitId));
	}

	@PostMapping
	public ResponseEntity<ProduitResponse> addProduit(@Valid @RequestBody Produit produit) {
		log.info("Creating product '{}' through REST API", produit.getLibelleProduit());
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(toResponse(produitService.addProduit(produit)));
	}

	@DeleteMapping("/{produitId}")
	public ResponseEntity<Void> removeProduit(@PathVariable Long produitId) {
		log.info("Deleting product {} through REST API", produitId);
		produitService.deleteProduit(produitId);
		return ResponseEntity.noContent().build();
	}

	@PutMapping("/{produitId}")
	public ResponseEntity<ProduitResponse> modifyProduit(
			@PathVariable Long produitId,
			@Valid @RequestBody Produit produit) {
		log.info("Updating product {} through REST API", produitId);
		produit.setIdProduit(produitId);
		return ResponseEntity.ok(toResponse(produitService.updateProduit(produit)));
	}

	@PutMapping("/{produitId}/stock/{stockId}")
	public ResponseEntity<ProduitResponse> assignProduitToStock(
			@PathVariable Long produitId,
			@PathVariable Long stockId) {
		log.info("Assigning product {} to stock {} through REST API", produitId, stockId);
		produitService.assignProduitToStock(produitId, stockId);
		return ResponseEntity.ok(toResponse(produitService.retrieveProduit(produitId)));
	}

	private ProduitResponse toResponse(Produit produit) {
		return new ProduitResponse(
				produit.getIdProduit(),
				produit.getCodeProduit(),
				produit.getLibelleProduit(),
				produit.getPrix(),
				produit.getStock() == null ? null : produit.getStock().getIdStock(),
				produit.getCategorieProduit() == null
						? null
						: produit.getCategorieProduit().getIdCategorieProduit());
	}
}
