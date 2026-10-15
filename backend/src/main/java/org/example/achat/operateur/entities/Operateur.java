package org.example.achat.operateur.entities;

import java.io.Serializable;
import java.util.Set;
import org.example.achat.facture.entities.Facture;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Operateur implements Serializable{
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long idOperateur;
	@NotBlank
	private String nom;
	@NotBlank
	private String prenom;
	
	@NotBlank
	private String password;
	@OneToMany(fetch = jakarta.persistence.FetchType.LAZY)
	@JsonIgnore
	private Set<Facture> factures;
	
}
