package org.example.achat.produit.entities;


import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Set;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CategorieProduit implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)

	private Long idCategorieProduit;

	@NotBlank
	private String codeCategorie;

	@NotBlank
	private String libelleCategorie;

	@OneToMany(mappedBy = "categorieProduit")
	@JsonIgnore
	private Set<Produit> produits;
}
