package org.example.achat.operateur.repositories;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.example.achat.operateur.entities.Operateur;

import java.util.List;
import java.util.Optional;

@Repository
public interface OperateurRepository extends JpaRepository<Operateur, Long> {

	// Invoices are mapped to the response outside the persistence session (open-in-view is disabled).
	@Override
	@EntityGraph(attributePaths = "factures")
	List<Operateur> findAll();

	@Override
	@EntityGraph(attributePaths = "factures")
	Optional<Operateur> findById(Long id);
}
