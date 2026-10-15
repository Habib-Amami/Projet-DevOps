package org.example.achat.produit.controllers;

import jakarta.validation.Valid;
import org.example.achat.produit.dto.ProduitResponse;
import org.example.achat.produit.entities.Produit;
import org.example.achat.produit.services.IProduitService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin("*")
@RequestMapping("/produits")
public class ProduitRestController {

	private final IProduitService produitService;

	public ProduitRestController(IProduitService produitService) {
		this.produitService = produitService;
	}

	@GetMapping
	public List<ProduitResponse> getProduits() {
		return produitService.retrieveAllProduits().stream()
				.map(this::toResponse)
				.toList();
	}

	@GetMapping("/{produitId}")
	public ProduitResponse retrieveProduit(@PathVariable Long produitId) {
		return toResponse(produitService.retrieveProduit(produitId));
	}

	@PostMapping
	public ResponseEntity<ProduitResponse> addProduit(@Valid @RequestBody Produit produit) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(toResponse(produitService.addProduit(produit)));
	}

	@DeleteMapping("/{produitId}")
	public ResponseEntity<Void> removeProduit(@PathVariable Long produitId) {
		produitService.deleteProduit(produitId);
		return ResponseEntity.noContent().build();
	}

	@PutMapping("/{produitId}")
	public ResponseEntity<ProduitResponse> modifyProduit(
			@PathVariable Long produitId,
			@Valid @RequestBody Produit produit) {
		produit.setIdProduit(produitId);
		return ResponseEntity.ok(toResponse(produitService.updateProduit(produit)));
	}

	@PutMapping("/{produitId}/stock/{stockId}")
	public ResponseEntity<ProduitResponse> assignProduitToStock(
			@PathVariable Long produitId,
			@PathVariable Long stockId) {
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
