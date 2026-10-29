package org.example.achat.produit.controllers;

import jakarta.validation.Valid;
import org.example.achat.produit.dto.CategorieProduitResponse;
import org.example.achat.produit.dto.CategorieProduitRequest;
import org.example.achat.produit.entities.CategorieProduit;
import org.example.achat.produit.services.ICategorieProduitService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/categories-produits")
public class CategorieProduitController {

	private final ICategorieProduitService categorieProduitService;

	public CategorieProduitController(ICategorieProduitService categorieProduitService) {
		this.categorieProduitService = categorieProduitService;
	}

	@GetMapping
	public List<CategorieProduitResponse> getCategorieProduits() {
		return categorieProduitService.retrieveAllCategorieProduits().stream()
				.map(this::toResponse)
				.toList();
	}

	@GetMapping("/{categorieProduitId}")
	public CategorieProduitResponse retrieveCategorieProduit(@PathVariable Long categorieProduitId) {
		return toResponse(categorieProduitService.retrieveCategorieProduit(categorieProduitId));
	}

	@PostMapping
	public ResponseEntity<CategorieProduitResponse> addCategorieProduit(
			@Valid @RequestBody CategorieProduitRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(toResponse(categorieProduitService.addCategorieProduit(toEntity(request))));
	}

	@DeleteMapping("/{categorieProduitId}")
	public ResponseEntity<Void> removeCategorieProduit(@PathVariable Long categorieProduitId) {
		categorieProduitService.deleteCategorieProduit(categorieProduitId);
		return ResponseEntity.noContent().build();
	}

	@PutMapping("/{categorieProduitId}")
	public ResponseEntity<CategorieProduitResponse> modifyCategorieProduit(
			@PathVariable Long categorieProduitId,
			@Valid @RequestBody CategorieProduitRequest request) {
		CategorieProduit categorieProduit = toEntity(request);
		categorieProduit.setIdCategorieProduit(categorieProduitId);
		return ResponseEntity.ok(toResponse(categorieProduitService.updateCategorieProduit(categorieProduit)));
	}

	private CategorieProduit toEntity(CategorieProduitRequest request) {
		CategorieProduit categorieProduit = new CategorieProduit();
		categorieProduit.setCodeCategorie(request.codeCategorie());
		categorieProduit.setLibelleCategorie(request.libelleCategorie());
		return categorieProduit;
	}

	private CategorieProduitResponse toResponse(CategorieProduit categorieProduit) {
		return new CategorieProduitResponse(
				categorieProduit.getIdCategorieProduit(),
				categorieProduit.getCodeCategorie(),
				categorieProduit.getLibelleCategorie());
	}
}
