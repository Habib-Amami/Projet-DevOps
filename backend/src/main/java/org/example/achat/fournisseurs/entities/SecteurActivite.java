package org.example.achat.fournisseurs.entities;

import java.io.Serializable;
import java.util.Set;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
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
public class SecteurActivite implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long idSecteurActivite;
	@NotBlank
	private String codeSecteurActivite;
	@NotBlank
	private String libelleSecteurActivite;
	@ManyToMany(mappedBy="secteurActivites")
	@JsonIgnore
	private Set<Fournisseur> fournisseurs;
}
