package org.example.achat.stock.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.achat.produit.entities.Produit;

import java.io.Serial;
import java.io.Serializable;
import java.util.Set;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Stock implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long idStock;

	@NotBlank
	private String libelleStock;

	@NotNull
	@PositiveOrZero
	private Integer qte;

	@NotNull
	@PositiveOrZero
	private Integer qteMin;

	@OneToMany(mappedBy = "stock")
	@JsonIgnore
	private Set<Produit> produits;
}
