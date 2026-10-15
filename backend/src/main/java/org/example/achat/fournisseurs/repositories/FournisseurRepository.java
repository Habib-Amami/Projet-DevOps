package org.example.achat.fournisseurs.repositories;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.example.achat.fournisseurs.entities.Fournisseur;

import java.util.List;
import java.util.Optional;

@Repository
public interface FournisseurRepository extends JpaRepository<Fournisseur, Long> {

	// Sectors are mapped to the response outside the persistence session (open-in-view is disabled).
	@Override
	@EntityGraph(attributePaths = "secteurActivites")
	List<Fournisseur> findAll();

	@Override
	@EntityGraph(attributePaths = "secteurActivites")
	Optional<Fournisseur> findById(Long id);
}
