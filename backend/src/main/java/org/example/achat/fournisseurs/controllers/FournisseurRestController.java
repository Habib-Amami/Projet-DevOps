package org.example.achat.fournisseurs.controllers;

import jakarta.validation.Valid;
import jakarta.validation.groups.Default;
import org.example.achat.fournisseurs.dto.DetailFournisseurRequest;
import org.example.achat.fournisseurs.dto.FournisseurRequest;
import org.example.achat.fournisseurs.dto.FournisseurResponse;
import org.example.achat.fournisseurs.entities.DetailFournisseur;
import org.example.achat.fournisseurs.entities.Fournisseur;
import org.example.achat.fournisseurs.services.IFournisseurService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/fournisseurs")
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
	public ResponseEntity<FournisseurResponse> addFournisseur(
			@Validated({Default.class, FournisseurRequest.Create.class}) @RequestBody FournisseurRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(toResponse(fournisseurService.addFournisseur(toEntity(request))));
	}

	@DeleteMapping("/{fournisseurId}")
	public ResponseEntity<Void> removeFournisseur(@PathVariable Long fournisseurId) {
		fournisseurService.deleteFournisseur(fournisseurId);
		return ResponseEntity.noContent().build();
	}

	@PutMapping("/{fournisseurId}")
	public ResponseEntity<FournisseurResponse> modifyFournisseur(
			@PathVariable Long fournisseurId,
			@Valid @RequestBody FournisseurRequest request) {
		Fournisseur fournisseur = toEntity(request);
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

	private Fournisseur toEntity(FournisseurRequest request) {
		Fournisseur fournisseur = new Fournisseur();
		fournisseur.setCode(request.code());
		fournisseur.setLibelle(request.libelle());
		fournisseur.setCategorieFournisseur(request.categorieFournisseur());
		if (request.detailFournisseur() != null) {
			DetailFournisseurRequest detailRequest = request.detailFournisseur();
			DetailFournisseur detail = new DetailFournisseur();
			detail.setEmail(detailRequest.email());
			detail.setDateDebutCollaboration(detailRequest.dateDebutCollaboration());
			detail.setAdresse(detailRequest.adresse());
			detail.setMatricule(detailRequest.matricule());
			fournisseur.setDetailFournisseur(detail);
		}
		return fournisseur;
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
