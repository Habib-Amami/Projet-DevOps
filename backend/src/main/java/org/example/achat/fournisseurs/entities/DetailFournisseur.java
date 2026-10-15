package org.example.achat.fournisseurs.entities;

import java.io.Serializable;
import java.util.Date;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.validation.constraints.Email;
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
public class DetailFournisseur implements Serializable{
	private static final long serialVersionUID = 1L;
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long idDetailFournisseur;
	@Email
	private String email;
	@Temporal(TemporalType.DATE)
	private Date dateDebutCollaboration;
	@NotBlank
	private String adresse;
	@NotBlank
	private String matricule;
	@OneToOne(mappedBy="detailFournisseur")
	@JsonIgnore
	private Fournisseur fournisseur;
	
}
