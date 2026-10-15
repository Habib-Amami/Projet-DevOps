package org.example.achat.facture.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.example.achat.facture.entities.DetailFacture;

@Repository
public interface DetailFactureRepository extends JpaRepository<DetailFacture, Long> {

}
