package org.example.achat.reglements.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.Date;

import org.example.achat.facture.entities.Facture;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Reglement implements Serializable{
	@Serial
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long idReglement;

	@PositiveOrZero
	private float montantPaye;

	@PositiveOrZero
	private float montantRestant;

	@NotNull
	private Boolean payee;

	@Temporal(TemporalType.DATE)
	@NotNull
	private Date dateReglement;

	@ManyToOne(fetch = FetchType.LAZY)
	@NotNull
	@JsonIgnore
	private Facture facture;
}
