package org.example.achat.facture.services;

import org.example.achat.facture.entities.Facture;
import org.example.achat.facture.dto.FactureRequest;

import java.util.Date;
import java.util.List;

public interface IFactureService {
	List<Facture> retrieveAllFactures();

	List<Facture> getFacturesByFournisseur(Long idFournisseur);

	Facture addFacture(Facture f);

	Facture createFacture(FactureRequest request);

	void cancelFacture(Long id);

	Facture retrieveFacture(Long id);
	
	void assignOperateurToFacture(Long idOperateur, Long idFacture);

	float pourcentageRecouvrement(Date startDate, Date endDate);
}
