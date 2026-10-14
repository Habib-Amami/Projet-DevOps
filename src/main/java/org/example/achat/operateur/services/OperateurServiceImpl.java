package org.example.achat.operateur.services;

import org.springframework.stereotype.Service;
import org.example.achat.operateur.entities.Operateur;
import org.example.achat.operateur.exceptions.OperateurNotFoundException;
import org.example.achat.operateur.repositories.OperateurRepository;

import java.util.List;

@Service
public class OperateurServiceImpl implements IOperateurService {

	private final OperateurRepository operateurRepository;

	public OperateurServiceImpl(OperateurRepository operateurRepository) {
		this.operateurRepository = operateurRepository;
	}
	@Override
	public List<Operateur> retrieveAllOperateurs() {
		return operateurRepository.findAll();
	}

	@Override
	public Operateur addOperateur(Operateur o) {
		return operateurRepository.save(o);
	}

	@Override
	public void deleteOperateur(Long id) {
		if (!operateurRepository.existsById(id)) {
			throw new OperateurNotFoundException(id);
		}
		operateurRepository.deleteById(id);
		
	}

	@Override
	public Operateur updateOperateur(Operateur o) {
		Operateur existing = operateurRepository.findById(o.getIdOperateur())
				.orElseThrow(() -> new OperateurNotFoundException(o.getIdOperateur()));
		existing.setNom(o.getNom());
		existing.setPrenom(o.getPrenom());
		existing.setPassword(o.getPassword());
		return operateurRepository.save(existing);
	}

	@Override
	public Operateur retrieveOperateur(Long id) {
		return operateurRepository.findById(id)
				.orElseThrow(() -> new OperateurNotFoundException(id));
	}

}
