package org.example.achat.facture.services;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.example.achat.facture.dto.FactureRequest;
import org.example.achat.facture.dto.DetailFactureRequest;
import org.example.achat.facture.entities.Facture;
import org.example.achat.facture.entities.DetailFacture;
import org.example.achat.facture.repositories.FactureRepository;
import org.example.achat.facture.repositories.DetailFactureRepository;
import org.example.achat.produit.repositories.ProduitRepository;
import org.example.achat.produit.entities.Produit;
import org.example.achat.operateur.entities.Operateur;
import org.example.achat.operateur.exceptions.OperateurNotFoundException;
import org.example.achat.operateur.repositories.OperateurRepository;
import org.example.achat.fournisseurs.exceptions.FournisseurNotFoundException;
import org.example.achat.fournisseurs.repositories.FournisseurRepository;
import org.example.achat.fournisseurs.entities.Fournisseur;
import org.example.achat.produit.exceptions.ProduitNotFoundException;
import org.example.achat.reglements.services.ReglementServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashSet;
import java.util.List;

@Service
@Slf4j
@Transactional
public class FactureServiceImpl implements IFactureService {

	@Autowired
    FactureRepository factureRepository;
	@Autowired
	OperateurRepository operateurRepository;
	@Autowired
    DetailFactureRepository detailFactureRepository;
	@Autowired
	FournisseurRepository fournisseurRepository;
	@Autowired
    ProduitRepository produitRepository;
    @Autowired
    ReglementServiceImpl reglementService;
	
	@Override
	public List<Facture> retrieveAllFactures() {
		List<Facture> factures = (List<Facture>) factureRepository.findAll();
		for (Facture facture : factures) {
			log.info(" facture : " + facture);
		}
		return factures;
	}

	
	public Facture addFacture(Facture f) {
		return factureRepository.save(f);
	}

	@Override
	public Facture createFacture(FactureRequest request) {
		Fournisseur fournisseur = fournisseurRepository.findById(request.idFournisseur())
				.orElseThrow(() -> new FournisseurNotFoundException(request.idFournisseur()));

		Date now = new Date();
		Facture facture = new Facture();
		facture.setFournisseur(fournisseur);
		facture.setDateCreationFacture(now);
		facture.setDateDerniereModificationFacture(now);
		facture.setArchivee(false);
		facture.setDetailsFacture(new HashSet<>());
		facture = factureRepository.save(facture);

		float montantFacture = 0;
		float montantRemise = 0;
		for (DetailFactureRequest detailRequest : request.detailsFacture()) {
			Produit produit = produitRepository.findById(detailRequest.idProduit())
					.orElseThrow(() -> new ProduitNotFoundException(detailRequest.idProduit()));

			DetailFacture detail = new DetailFacture();
			detail.setProduit(produit);
			detail.setFacture(facture);
			detail.setQteCommandee(detailRequest.qteCommandee());
			detail.setPourcentageRemise(detailRequest.pourcentageRemise());

			float prixTotalDetail = detail.getQteCommandee() * produit.getPrix();
			float montantRemiseDetail = (prixTotalDetail * detail.getPourcentageRemise()) / 100;
			float prixTotalDetailRemise = prixTotalDetail - montantRemiseDetail;
			detail.setMontantRemise(montantRemiseDetail);
			detail.setPrixTotalDetail(prixTotalDetailRemise);

			detailFactureRepository.save(detail);
			facture.getDetailsFacture().add(detail);
			montantFacture = montantFacture + prixTotalDetailRemise;
			montantRemise = montantRemise + montantRemiseDetail;
		}
		facture.setMontantFacture(montantFacture);
		facture.setMontantRemise(montantRemise);
		return factureRepository.save(facture);
	}

	@Override
	public void cancelFacture(Long factureId) {
		// Méthode 01
		//Facture facture = factureRepository.findById(factureId).get();
		Facture facture = factureRepository.findById(factureId).orElse(new Facture());
		facture.setArchivee(true);
		factureRepository.save(facture);
		//Méthode 02 (Avec JPQL)
		factureRepository.updateFacture(factureId);
	}

	@Override
	public Facture retrieveFacture(Long factureId) {

		Facture facture = factureRepository.findById(factureId).orElse(null);
		log.info("facture :" + facture);
		return facture;
	}

	@Override
	public List<Facture> getFacturesByFournisseur(Long idFournisseur) {
		if (!fournisseurRepository.existsById(idFournisseur)) {
			throw new FournisseurNotFoundException(idFournisseur);
		}
		return factureRepository.findByFournisseurIdFournisseur(idFournisseur);
	}

	@Override
	public void assignOperateurToFacture(Long idOperateur, Long idFacture) {
		Facture facture = factureRepository.findById(idFacture).orElse(null);
		Operateur operateur = operateurRepository.findById(idOperateur)
				.orElseThrow(() -> new OperateurNotFoundException(idOperateur));
		operateur.getFactures().add(facture);
		operateurRepository.save(operateur);
	}

	@Override
	public float pourcentageRecouvrement(Date startDate, Date endDate) {
		float totalFacturesEntreDeuxDates = factureRepository.getTotalFacturesEntreDeuxDates(startDate,endDate);
		float totalRecouvrementEntreDeuxDates = reglementService.getRevenueBetweenDates(startDate, endDate);
		float pourcentage=(totalRecouvrementEntreDeuxDates/totalFacturesEntreDeuxDates)*100;
		return pourcentage;
	}
	

}