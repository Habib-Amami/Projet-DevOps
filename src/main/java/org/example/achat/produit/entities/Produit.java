package org.example.achat.produit.entities;

import org.example.achat.stock.entities.Stock;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

import java.util.Date;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Produit implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long idProduit;

	@NotBlank
	private String codeProduit;

	@NotBlank
	private String libelleProduit;

	@NotNull
	@PositiveOrZero
	private float prix;

	@Temporal(TemporalType.DATE)
	private Date dateCreation;

	@Temporal(TemporalType.DATE)
	private Date dateDerniereModification;

	@ManyToOne(fetch = FetchType.LAZY)
	@JsonIgnore
	private Stock stock;

	//@OneToMany(mappedBy = "produit")
	//@JsonIgnore
	//private Set<DetailFacture> detailFacture;

	@ManyToOne(fetch = FetchType.LAZY)
	@JsonIgnore
	private CategorieProduit categorieProduit;
	


	

}
