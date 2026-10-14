package org.example.achat.fournisseurs.controllers;

import jakarta.validation.Valid;
import org.example.achat.fournisseurs.dto.FournisseurResponse;
import org.example.achat.fournisseurs.entities.Fournisseur;
import org.example.achat.fournisseurs.services.IFournisseurService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/fournisseurs")
@CrossOrigin("*")
public class FournisseurRestController {

	private final IFournisseurService fournisseurService;

	public FournisseurRestController(IFournisseurService fournisseurService) {
		this.fournisseurService = fournisseurService;
	}

	@GetMapping
	public List<FournisseurResponse> getFournisseurs() {
		return fournisseurService.retrieveAllFournisseurs().stream()
				.map(this::toResponse)
				.toList();
	}

	@GetMapping("/{fournisseurId}")
	public FournisseurResponse retrieveFournisseur(@PathVariable Long fournisseurId) {
		return toResponse(fournisseurService.retrieveFournisseur(fournisseurId));
	}

	@PostMapping
	public ResponseEntity<FournisseurResponse> addFournisseur(@Valid @RequestBody Fournisseur fournisseur) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(toResponse(fournisseurService.addFournisseur(fournisseur)));
	}

	@DeleteMapping("/{fournisseurId}")
	public ResponseEntity<Void> removeFournisseur(@PathVariable Long fournisseurId) {
		fournisseurService.deleteFournisseur(fournisseurId);
		return ResponseEntity.noContent().build();
	}

	@PutMapping("/{fournisseurId}")
	public ResponseEntity<FournisseurResponse> modifyFournisseur(
			@PathVariable Long fournisseurId,
			@Valid @RequestBody Fournisseur fournisseur) {
		fournisseur.setIdFournisseur(fournisseurId);
		return ResponseEntity.ok(toResponse(fournisseurService.updateFournisseur(fournisseur)));
	}

	@PutMapping("/{fournisseurId}/secteurs-activite/{secteurActiviteId}")
	public FournisseurResponse assignSecteurActivite(
			@PathVariable Long fournisseurId,
			@PathVariable Long secteurActiviteId) {
		fournisseurService.assignSecteurActiviteToFournisseur(secteurActiviteId, fournisseurId);
		return toResponse(fournisseurService.retrieveFournisseur(fournisseurId));
	}

	private FournisseurResponse toResponse(Fournisseur fournisseur) {
		return new FournisseurResponse(
				fournisseur.getIdFournisseur(),
				fournisseur.getCode(),
				fournisseur.getLibelle(),
				fournisseur.getCategorieFournisseur(),
				fournisseur.getDetailFournisseur() == null
						? null : fournisseur.getDetailFournisseur().getIdDetailFournisseur(),
				fournisseur.getSecteurActivites() == null
						? Collections.emptySet()
						: fournisseur.getSecteurActivites().stream()
								.map(secteur -> secteur.getIdSecteurActivite())
								.collect(java.util.stream.Collectors.toSet()));
	}
}
