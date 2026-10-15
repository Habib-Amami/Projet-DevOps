package org.example.achat.reglements.services;

import org.example.achat.reglements.entities.Reglement;

import java.util.Date;
import java.util.List;

public interface IReglementService {

	List<Reglement> retrieveAllReglements();

	Reglement addReglement(Reglement r);

	Reglement retrieveReglement(Long id);

	Reglement updateReglement(Reglement r);

	void deleteReglement(Long id);

	List<Reglement> findByFactureId(Long idFacture);

	float getRevenueBetweenDates(Date startDate, Date endDate);
}
