package org.example.achat.operateur.controllers;

import jakarta.validation.Valid;
import org.example.achat.operateur.dto.OperateurResponse;
import org.example.achat.operateur.dto.OperateurRequest;
import org.example.achat.operateur.entities.Operateur;
import org.example.achat.operateur.services.IOperateurService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/operateurs")
public class OperateurController {

	private final IOperateurService operateurService;

	public OperateurController(IOperateurService operateurService) {
		this.operateurService = operateurService;
	}

	@GetMapping
	public List<OperateurResponse> getOperateurs() {
		return operateurService.retrieveAllOperateurs().stream()
				.map(this::toResponse)
				.toList();
	}

	@GetMapping("/{operateurId}")
	public OperateurResponse retrieveOperateur(@PathVariable Long operateurId) {
		return toResponse(operateurService.retrieveOperateur(operateurId));
	}

	@PostMapping
	public ResponseEntity<OperateurResponse> addOperateur(@Valid @RequestBody OperateurRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(toResponse(operateurService.addOperateur(toEntity(request))));
	}

	@DeleteMapping("/{operateurId}")
	public ResponseEntity<Void> removeOperateur(@PathVariable Long operateurId) {
		operateurService.deleteOperateur(operateurId);
		return ResponseEntity.noContent().build();
	}

	@PutMapping("/{operateurId}")
	public ResponseEntity<OperateurResponse> modifyOperateur(
			@PathVariable Long operateurId,
			@Valid @RequestBody OperateurRequest request) {
		Operateur operateur = toEntity(request);
		operateur.setIdOperateur(operateurId);
		return ResponseEntity.ok(toResponse(operateurService.updateOperateur(operateur)));
	}

	private Operateur toEntity(OperateurRequest request) {
		Operateur operateur = new Operateur();
		operateur.setNom(request.nom());
		operateur.setPrenom(request.prenom());
		operateur.setPassword(request.password());
		return operateur;
	}

	private OperateurResponse toResponse(Operateur operateur) {
		return new OperateurResponse(
				operateur.getIdOperateur(),
				operateur.getNom(),
				operateur.getPrenom(),
				operateur.getFactures() == null
						? Collections.emptySet()
						: operateur.getFactures().stream()
								.map(facture -> facture.getIdFacture())
								.collect(java.util.stream.Collectors.toSet()));
	}
}
