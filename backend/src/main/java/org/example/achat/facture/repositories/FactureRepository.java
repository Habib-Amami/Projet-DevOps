package org.example.achat.facture.repositories;

import org.example.achat.facture.entities.Facture;
import org.example.achat.fournisseurs.entities.Fournisseur;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface FactureRepository extends JpaRepository<Facture, Long> {

	// Invoices are serialized with their details outside the persistence session (open-in-view is disabled).
	@Override
	@EntityGraph(attributePaths = {"detailsFacture", "detailsFacture.produit"})
	List<Facture> findAll();

	@Override
	@EntityGraph(attributePaths = {"detailsFacture", "detailsFacture.produit"})
	Optional<Facture> findById(Long id);

	@EntityGraph(attributePaths = {"detailsFacture", "detailsFacture.produit"})
	List<Facture> findByFournisseurIdFournisseur(Long idFournisseur);

	@Query("SELECT f FROM Facture f where f.fournisseur=:fournisseur and f.archivee=false")
	public List<Facture> getFactureByFournisseur(@Param("fournisseur") Fournisseur fournisseur);

	
	@Query("SELECT sum(f.montantFacture) FROM Facture f where  f.dateCreationFacture between :startDate"
			+ " and :endDate and f.archivee=false")
	float getTotalFacturesEntreDeuxDates(@Param("startDate") Date startDate, @Param("endDate") Date endDate);

	@Modifying
	@Query("update Facture f set f.archivee=true where f.idFacture=?1")
	void updateFacture(Long id);
	
}
