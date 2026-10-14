package org.example.achat.reglements.services;

import org.example.achat.reglements.entities.Reglement;
import org.example.achat.reglements.exceptions.ReglementNotFoundException;
import org.example.achat.reglements.repositories.ReglementRepository;
import org.springframework.stereotype.Service;


import java.util.Date;
import java.util.List;

@Service
public class ReglementServiceImpl implements IReglementService {

	private final ReglementRepository reglementRepository;

	public ReglementServiceImpl(ReglementRepository reglementRepository) {
		this.reglementRepository = reglementRepository;
	}

	@Override
	public List<Reglement> retrieveAllReglements() {
		return reglementRepository.findAll();
	}

	@Override
	public Reglement addReglement(Reglement r) {
		return reglementRepository.save(r);
	}

	@Override
	public Reglement retrieveReglement(Long id) {
		return reglementRepository.findById(id)
				.orElseThrow(() -> new ReglementNotFoundException(id));
	}

	@Override
	public Reglement updateReglement(Reglement r) {
		Reglement existing = reglementRepository.findById(r.getIdReglement())
				.orElseThrow(() -> new ReglementNotFoundException(r.getIdReglement()));
		existing.setMontantPaye(r.getMontantPaye());
		existing.setMontantRestant(r.getMontantRestant());
		existing.setPayee(r.getPayee());
		existing.setDateReglement(r.getDateReglement());
		return reglementRepository.save(existing);
	}

	@Override
	public void deleteReglement(Long id) {
		if (!reglementRepository.existsById(id)) {
			throw new ReglementNotFoundException(id);
		}
		reglementRepository.deleteById(id);
	}

	@Override
	public List<Reglement> findByFactureId(Long idFacture) {
		return reglementRepository.findByFactureId(idFacture);
	}

	@Override
	public float getRevenueBetweenDates(Date startDate, Date endDate) {
		if (startDate.after(endDate)) {
			throw new IllegalArgumentException("Start date must be before or equal to end date");
		}
		Float revenue = reglementRepository.getRevenueBetweenDates(startDate, endDate);
		return revenue == null ? 0.0f : revenue;
	}
}
