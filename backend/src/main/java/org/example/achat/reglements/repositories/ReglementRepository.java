package org.example.achat.reglements.repositories;

import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;

import org.example.achat.reglements.entities.Reglement;

@Repository
public interface ReglementRepository extends JpaRepository<Reglement, Long>{
	@Query("SELECT r FROM Reglement r where r.facture.idFacture=:idFacture")
	List<Reglement> findByFactureId(@Param("idFacture") Long idFacture);

	
	@Query("SELECT sum(r.montantPaye) FROM Reglement r where  r.dateReglement between :startDate"
			+ " and :endDate and r.facture.archivee=false")
	Float getRevenueBetweenDates(@Param("startDate") Date startDate, @Param("endDate") Date endDate);
}
