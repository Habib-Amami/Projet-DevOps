package org.example.achat.fournisseurs.services;

import org.springframework.stereotype.Service;
import org.example.achat.fournisseurs.entities.DetailFournisseur;
import org.example.achat.fournisseurs.entities.Fournisseur;
import org.example.achat.fournisseurs.entities.SecteurActivite;
import org.example.achat.fournisseurs.repositories.DetailFournisseurRepository;
import org.example.achat.fournisseurs.repositories.FournisseurRepository;
import org.example.achat.fournisseurs.repositories.SecteurActiviteRepository;
import org.example.achat.fournisseurs.exceptions.FournisseurNotFoundException;
import org.example.achat.fournisseurs.exceptions.SecteurActiviteNotFoundException;

import java.util.Date;
import java.util.List;
import java.util.HashSet;

@Service
public class FournisseurServiceImpl implements IFournisseurService {

	private final FournisseurRepository fournisseurRepository;
	private final DetailFournisseurRepository detailFournisseurRepository;
	private final SecteurActiviteRepository secteurActiviteRepository;

	public FournisseurServiceImpl(
			FournisseurRepository fournisseurRepository,
			DetailFournisseurRepository detailFournisseurRepository,
			SecteurActiviteRepository secteurActiviteRepository) {
		this.fournisseurRepository = fournisseurRepository;
		this.detailFournisseurRepository = detailFournisseurRepository;
		this.secteurActiviteRepository = secteurActiviteRepository;
	}

	@Override
	public List<Fournisseur> retrieveAllFournisseurs() {
		return fournisseurRepository.findAll();
	}


	public Fournisseur addFournisseur(Fournisseur f) {
		if (f.getDetailFournisseur() == null) {
			DetailFournisseur detail = new DetailFournisseur();
			detail.setDateDebutCollaboration(new Date());
			f.setDetailFournisseur(detail);
		}
		return fournisseurRepository.save(f);
	}
	
	public Fournisseur updateFournisseur(Fournisseur f) {
		Fournisseur existing = fournisseurRepository.findById(f.getIdFournisseur())
				.orElseThrow(() -> new FournisseurNotFoundException(f.getIdFournisseur()));
		existing.setCode(f.getCode());
		existing.setLibelle(f.getLibelle());
		existing.setCategorieFournisseur(f.getCategorieFournisseur());
		if (f.getDetailFournisseur() != null) {
			existing.setDetailFournisseur(f.getDetailFournisseur());
		}
		return fournisseurRepository.save(existing);
	}

	@Override
	public void deleteFournisseur(Long fournisseurId) {
		if (!fournisseurRepository.existsById(fournisseurId)) {
			throw new FournisseurNotFoundException(fournisseurId);
		}
		fournisseurRepository.deleteById(fournisseurId);

	}

	@Override
	public Fournisseur retrieveFournisseur(Long fournisseurId) {

		return fournisseurRepository.findById(fournisseurId)
				.orElseThrow(() -> new FournisseurNotFoundException(fournisseurId));
	}

	@Override
	public void assignSecteurActiviteToFournisseur(Long idSecteurActivite, Long idFournisseur) {
		Fournisseur fournisseur = fournisseurRepository.findById(idFournisseur)
				.orElseThrow(() -> new FournisseurNotFoundException(idFournisseur));
		SecteurActivite secteurActivite = secteurActiviteRepository.findById(idSecteurActivite)
				.orElseThrow(() -> new SecteurActiviteNotFoundException(idSecteurActivite));
		if (fournisseur.getSecteurActivites() == null) {
			fournisseur.setSecteurActivites(new HashSet<>());
		}
		fournisseur.getSecteurActivites().add(secteurActivite);
        fournisseurRepository.save(fournisseur);
		
		
	}

	

}