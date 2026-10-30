package org.example.achat.fournisseurs.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.Date;

public record DetailFournisseurRequest(
		@Email String email,
		Date dateDebutCollaboration,
		@NotBlank String adresse,
		@NotBlank String matricule) {
}
