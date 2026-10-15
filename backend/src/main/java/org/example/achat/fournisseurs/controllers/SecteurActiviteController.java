package org.example.achat.fournisseurs.controllers;

import jakarta.validation.Valid;
import org.example.achat.fournisseurs.dto.SecteurActiviteResponse;
import org.example.achat.fournisseurs.entities.SecteurActivite;
import org.example.achat.fournisseurs.services.ISecteurActiviteService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/secteurs-activite")
@CrossOrigin("*")
public class SecteurActiviteController {

	private final ISecteurActiviteService secteurActiviteService;

	public SecteurActiviteController(ISecteurActiviteService secteurActiviteService) {
		this.secteurActiviteService = secteurActiviteService;
	}

	@GetMapping
	public List<SecteurActiviteResponse> getSecteursActivite() {
		return secteurActiviteService.retrieveAllSecteurActivite().stream()
				.map(this::toResponse)
				.toList();
	}

	@GetMapping("/{secteurActiviteId}")
	public SecteurActiviteResponse retrieveSecteurActivite(@PathVariable Long secteurActiviteId) {
		return toResponse(secteurActiviteService.retrieveSecteurActivite(secteurActiviteId));
	}

	@PostMapping
	public ResponseEntity<SecteurActiviteResponse> addSecteurActivite(
			@Valid @RequestBody SecteurActivite secteurActivite) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(toResponse(secteurActiviteService.addSecteurActivite(secteurActivite)));
	}

	@DeleteMapping("/{secteurActiviteId}")
	public ResponseEntity<Void> removeSecteurActivite(@PathVariable Long secteurActiviteId) {
		secteurActiviteService.deleteSecteurActivite(secteurActiviteId);
		return ResponseEntity.noContent().build();
	}

	@PutMapping("/{secteurActiviteId}")
	public ResponseEntity<SecteurActiviteResponse> modifySecteurActivite(
			@PathVariable Long secteurActiviteId,
			@Valid @RequestBody SecteurActivite secteurActivite) {
		secteurActivite.setIdSecteurActivite(secteurActiviteId);
		return ResponseEntity.ok(toResponse(secteurActiviteService.updateSecteurActivite(secteurActivite)));
	}

	private SecteurActiviteResponse toResponse(SecteurActivite secteurActivite) {
		return new SecteurActiviteResponse(
				secteurActivite.getIdSecteurActivite(),
				secteurActivite.getCodeSecteurActivite(),
				secteurActivite.getLibelleSecteurActivite());
	}
}
