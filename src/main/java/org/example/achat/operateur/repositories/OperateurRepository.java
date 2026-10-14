package org.example.achat.operateur.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.example.achat.operateur.entities.Operateur;

@Repository
public interface OperateurRepository extends JpaRepository<Operateur, Long> {

}
