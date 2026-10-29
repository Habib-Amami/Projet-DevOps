package org.example.achat.reglements.controllers;

import jakarta.validation.Valid;
import jakarta.validation.groups.Default;

import org.example.achat.facture.entities.Facture;
import org.example.achat.facture.services.IFactureService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

import org.example.achat.reglements.dto.ReglementRequest;
import org.example.achat.reglements.dto.ReglementResponse;
import org.example.achat.reglements.entities.Reglement;
import org.example.achat.reglements.services.IReglementService;

import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/reglements")
@CrossOrigin("*")
public class ReglementRestController {

	private final IReglementService reglementService;
	private final IFactureService factureService;

	public ReglementRestController(IReglementService reglementService, IFactureService factureService) {
		this.reglementService = reglementService;
		this.factureService = factureService;
	}

	@GetMapping
	public List<ReglementResponse> getReglements() {
		return reglementService.retrieveAllReglements().stream()
				.map(this::toResponse)
				.toList();
	}

	@GetMapping("/{reglementId}")
	public ReglementResponse retrieveReglement(@PathVariable Long reglementId) {
		return toResponse(reglementService.retrieveReglement(reglementId));
	}

	@PostMapping
	public ResponseEntity<ReglementResponse> addReglement(
			@Validated({Default.class, ReglementRequest.Create.class}) @RequestBody ReglementRequest request) {
		Reglement reglement = toEntity(request);
		Facture facture = factureService.retrieveFacture(request.factureId());
		if (facture == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Facture not found with id: " + request.factureId());
		}
		reglement.setFacture(facture);
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(toResponse(reglementService.addReglement(reglement)));
	}

	@PutMapping("/{reglementId}")
	public ResponseEntity<ReglementResponse> updateReglement(
			@PathVariable Long reglementId,
			@Valid @RequestBody ReglementRequest request) {
		Reglement reglement = toEntity(request);
		reglement.setIdReglement(reglementId);
		return ResponseEntity.ok(toResponse(reglementService.updateReglement(reglement)));
	}

	@DeleteMapping("/{reglementId}")
	public ResponseEntity<Void> deleteReglement(@PathVariable Long reglementId) {
		reglementService.deleteReglement(reglementId);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/facture/{factureId}")
	public List<ReglementResponse> findByFacture(@PathVariable Long factureId) {
		return reglementService.findByFactureId(factureId).stream()
				.map(this::toResponse)
				.toList();
	}

	@GetMapping("/revenue")
	public float getRevenueBetweenDates(
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date startDate,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date endDate) {
		return reglementService.getRevenueBetweenDates(startDate, endDate);
	}

	private Reglement toEntity(ReglementRequest request) {
		Reglement reglement = new Reglement();
		reglement.setMontantPaye(request.montantPaye());
		reglement.setMontantRestant(request.montantRestant());
		reglement.setPayee(request.payee());
		reglement.setDateReglement(request.dateReglement());
		return reglement;
	}

	private ReglementResponse toResponse(Reglement reglement) {
		return new ReglementResponse(
				reglement.getIdReglement(),
				reglement.getMontantPaye(),
				reglement.getMontantRestant(),
				reglement.getPayee(),
				reglement.getDateReglement(),
				reglement.getFacture() == null ? null : reglement.getFacture().getIdFacture());
	}
}
