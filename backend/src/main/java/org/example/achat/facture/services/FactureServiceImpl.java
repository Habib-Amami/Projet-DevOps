package org.example.achat.facture.services;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
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
import org.example.achat.reglements.services.ReglementServiceImpl;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
@Transactional
public class FactureServiceImpl implements IFactureService {

	private final FactureRepository factureRepository;
	private final OperateurRepository operateurRepository;
	private final DetailFactureRepository detailFactureRepository;
	private final FournisseurRepository fournisseurRepository;
	private final ProduitRepository produitRepository;
	private final ReglementServiceImpl reglementService;

	public FactureServiceImpl(
			FactureRepository factureRepository,
			OperateurRepository operateurRepository,
			DetailFactureRepository detailFactureRepository,
			FournisseurRepository fournisseurRepository,
			ProduitRepository produitRepository,
			ReglementServiceImpl reglementService) {
		this.factureRepository = factureRepository;
		this.operateurRepository = operateurRepository;
		this.detailFactureRepository = detailFactureRepository;
		this.fournisseurRepository = fournisseurRepository;
		this.produitRepository = produitRepository;
		this.reglementService = reglementService;
	}
	
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

	/*
	 * calculer les montants remise et le montant total d'un détail facture
	 * ainsi que les montants d'une facture
	 */
	private Facture addDetailsFacture(Facture f, Set<DetailFacture> detailsFacture) {
		float montantFacture = 0;
		float montantRemise = 0;
		for (DetailFacture detail : detailsFacture) {
			//Récuperer le produit 
			Produit produit = produitRepository.findById(detail.getProduit().getIdProduit()).get();
			//Calculer le montant total pour chaque détail Facture
			float prixTotalDetail = detail.getQteCommandee() * produit.getPrix();
			//Calculer le montant remise pour chaque détail Facture
			float montantRemiseDetail = (prixTotalDetail * detail.getPourcentageRemise()) / 100;
			float prixTotalDetailRemise = prixTotalDetail - montantRemiseDetail;
			detail.setMontantRemise(montantRemiseDetail);
			detail.setPrixTotalDetail(prixTotalDetailRemise);
			//Calculer le montant total pour la facture
			montantFacture = montantFacture + prixTotalDetailRemise;
			//Calculer le montant remise pour la facture
			montantRemise = montantRemise + montantRemiseDetail;
			detailFactureRepository.save(detail);
		}
		f.setMontantFacture(montantFacture);
		f.setMontantRemise(montantRemise);
		return f;
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
