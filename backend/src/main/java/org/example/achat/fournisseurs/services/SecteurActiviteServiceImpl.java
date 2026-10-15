package org.example.achat.fournisseurs.services;

import org.springframework.stereotype.Service;
import org.example.achat.fournisseurs.entities.SecteurActivite;
import org.example.achat.fournisseurs.repositories.SecteurActiviteRepository;
import org.example.achat.fournisseurs.exceptions.SecteurActiviteNotFoundException;

import java.util.List;

@Service
public class SecteurActiviteServiceImpl implements ISecteurActiviteService{

	private final SecteurActiviteRepository secteurActiviteRepository;

	public SecteurActiviteServiceImpl(SecteurActiviteRepository secteurActiviteRepository) {
		this.secteurActiviteRepository = secteurActiviteRepository;
	}
	@Override
	public List<SecteurActivite> retrieveAllSecteurActivite() {
		return secteurActiviteRepository.findAll();
	}

	@Override
	public SecteurActivite addSecteurActivite(SecteurActivite sa) {
		return secteurActiviteRepository.save(sa);
	}

	@Override
	public void deleteSecteurActivite(Long id) {
		if (!secteurActiviteRepository.existsById(id)) {
			throw new SecteurActiviteNotFoundException(id);
		}
		secteurActiviteRepository.deleteById(id);
		
	}

	@Override
	public SecteurActivite updateSecteurActivite(SecteurActivite sa) {
		SecteurActivite existing = secteurActiviteRepository.findById(sa.getIdSecteurActivite())
				.orElseThrow(() -> new SecteurActiviteNotFoundException(sa.getIdSecteurActivite()));
		existing.setCodeSecteurActivite(sa.getCodeSecteurActivite());
		existing.setLibelleSecteurActivite(sa.getLibelleSecteurActivite());
		return secteurActiviteRepository.save(existing);
	}

	@Override
	public SecteurActivite retrieveSecteurActivite(Long id) {
		return secteurActiviteRepository.findById(id)
				.orElseThrow(() -> new SecteurActiviteNotFoundException(id));
	}

}
