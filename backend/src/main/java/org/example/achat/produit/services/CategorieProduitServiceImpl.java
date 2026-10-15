package org.example.achat.produit.services;


import org.example.achat.produit.entities.CategorieProduit;
import org.example.achat.produit.repositories.CategorieProduitRepository;
import org.springframework.stereotype.Service;
import org.example.achat.produit.exceptions.CategorieProduitNotFoundException;

import java.util.List;

@Service
public class CategorieProduitServiceImpl implements ICategorieProduitService {

	
	private final CategorieProduitRepository categorieProduitRepository;

	public CategorieProduitServiceImpl(CategorieProduitRepository categorieProduitRepository) {
		this.categorieProduitRepository = categorieProduitRepository;
	}
	@Override
	public List<CategorieProduit> retrieveAllCategorieProduits() {
		
		return categorieProduitRepository.findAll();
	}

	@Override
	public CategorieProduit addCategorieProduit(CategorieProduit cp) {
		return categorieProduitRepository.save(cp);
	}

	@Override
	public void deleteCategorieProduit(Long id) {
		if (!categorieProduitRepository.existsById(id)) {
			throw new CategorieProduitNotFoundException(id);
		}
		categorieProduitRepository.deleteById(id);
		
	}

	@Override
	public CategorieProduit updateCategorieProduit(CategorieProduit cp) {
		CategorieProduit existing = categorieProduitRepository.findById(cp.getIdCategorieProduit())
				.orElseThrow(() -> new CategorieProduitNotFoundException(cp.getIdCategorieProduit()));
		existing.setCodeCategorie(cp.getCodeCategorie());
		existing.setLibelleCategorie(cp.getLibelleCategorie());
		return categorieProduitRepository.save(existing);
	}

	@Override
	public CategorieProduit retrieveCategorieProduit(Long id) {
		return categorieProduitRepository.findById(id)
				.orElseThrow(() -> new CategorieProduitNotFoundException(id));
	}

}
