package org.example.achat.operateur.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record OperateurRequest(
		@NotBlank String nom,
		@NotBlank String prenom,
		@NotBlank @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) String password) {

	@Override
	public String toString() {
		return "OperateurRequest[nom=" + nom + ", prenom=" + prenom + ", password=[REDACTED]]";
	}
}
