package org.example.achat.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.example.achat.produit.entities.CategorieProduit;
import org.example.achat.produit.entities.Produit;
import org.example.achat.produit.repositories.CategorieProduitRepository;
import org.example.achat.produit.repositories.ProduitRepository;
import org.example.achat.stock.entities.Stock;
import org.example.achat.stock.repositories.StockRepository;
import org.example.achat.fournisseurs.entities.CategorieFournisseur;
import org.example.achat.fournisseurs.entities.DetailFournisseur;
import org.example.achat.fournisseurs.entities.Fournisseur;
import org.example.achat.fournisseurs.entities.SecteurActivite;
import org.example.achat.fournisseurs.repositories.DetailFournisseurRepository;
import org.example.achat.fournisseurs.repositories.FournisseurRepository;
import org.example.achat.fournisseurs.repositories.SecteurActiviteRepository;
import org.example.achat.operateur.entities.Operateur;
import org.example.achat.operateur.repositories.OperateurRepository;
import org.example.achat.facture.entities.DetailFacture;
import org.example.achat.facture.entities.Facture;
import org.example.achat.facture.repositories.DetailFactureRepository;
import org.example.achat.facture.repositories.FactureRepository;
import org.example.achat.reglements.entities.Reglement;
import org.example.achat.reglements.repositories.ReglementRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Set;

@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class DevelopmentDataSeeder implements ApplicationRunner {

	private final ObjectMapper objectMapper;
	private final StockRepository stockRepository;
	private final CategorieProduitRepository categorieProduitRepository;
	private final ProduitRepository produitRepository;
	private final SecteurActiviteRepository secteurActiviteRepository;
	private final DetailFournisseurRepository detailFournisseurRepository;
	private final FournisseurRepository fournisseurRepository;
	private final OperateurRepository operateurRepository;
	private final FactureRepository factureRepository;
	private final DetailFactureRepository detailFactureRepository;
	private final ReglementRepository reglementRepository;

	public DevelopmentDataSeeder(
			StockRepository stockRepository,
			CategorieProduitRepository categorieProduitRepository,
			ProduitRepository produitRepository,
			SecteurActiviteRepository secteurActiviteRepository,
			DetailFournisseurRepository detailFournisseurRepository,
			FournisseurRepository fournisseurRepository,
			OperateurRepository operateurRepository,
			FactureRepository factureRepository,
			DetailFactureRepository detailFactureRepository,
			ReglementRepository reglementRepository) {
		this.objectMapper = JsonMapper.builder().build();
		this.stockRepository = stockRepository;
		this.categorieProduitRepository = categorieProduitRepository;
		this.produitRepository = produitRepository;
		this.secteurActiviteRepository = secteurActiviteRepository;
		this.detailFournisseurRepository = detailFournisseurRepository;
		this.fournisseurRepository = fournisseurRepository;
		this.operateurRepository = operateurRepository;
		this.factureRepository = factureRepository;
		this.detailFactureRepository = detailFactureRepository;
		this.reglementRepository = reglementRepository;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) throws IOException {
		seedStocks();
		seedCategories();
		seedProducts();
		seedSecteurs();
		seedFournisseurs();
		seedOperateurs();
		seedFactures();
	}

	private void seedSecteurs() throws IOException {
		List<SecteurActivite> secteurs = secteurActiviteRepository.findAll();
		for (SecteurSeed seed : read("seed/secteurs.json", new TypeReference<List<SecteurSeed>>() {
		})) {
			if (secteurs.stream().noneMatch(s -> s.getCodeSecteurActivite().equals(seed.code()))) {
				SecteurActivite secteur = new SecteurActivite();
				secteur.setCodeSecteurActivite(seed.code());
				secteur.setLibelleSecteurActivite(seed.label());
				secteurs.add(secteurActiviteRepository.save(secteur));
			}
		}
	}

	private void seedFournisseurs() throws IOException {
		List<SecteurActivite> secteurs = secteurActiviteRepository.findAll();
		List<Fournisseur> fournisseurs = fournisseurRepository.findAll();
		for (FournisseurSeed seed : read("seed/fournisseurs.json", new TypeReference<List<FournisseurSeed>>() {
		})) {
			if (fournisseurs.stream().anyMatch(f -> f.getCode().equals(seed.code()))) {
				continue;
			}
			DetailFournisseur detail = new DetailFournisseur();
			detail.setEmail(seed.email());
			detail.setDateDebutCollaboration(date(seed.startDate()));
			detail.setAdresse(seed.address());
			detail.setMatricule(seed.registrationNumber());
			detail = detailFournisseurRepository.save(detail);

			Set<SecteurActivite> matchingSecteurs = secteurs.stream()
					.filter(s -> seed.sectorCodes().contains(s.getCodeSecteurActivite()))
					.collect(java.util.stream.Collectors.toSet());
			Fournisseur fournisseur = new Fournisseur();
			fournisseur.setCode(seed.code());
			fournisseur.setLibelle(seed.label());
			fournisseur.setCategorieFournisseur(CategorieFournisseur.valueOf(seed.category()));
			fournisseur.setSecteurActivites(matchingSecteurs);
			fournisseur.setDetailFournisseur(detail);
			detail.setFournisseur(fournisseur);
			fournisseurs.add(fournisseurRepository.save(fournisseur));
		}
	}

	private void seedOperateurs() throws IOException {
		List<Operateur> operateurs = operateurRepository.findAll();
		for (OperateurSeed seed : read("seed/operateurs.json", new TypeReference<List<OperateurSeed>>() {
		})) {
			if (operateurs.stream().anyMatch(o -> o.getNom().equals(seed.lastName())
					&& o.getPrenom().equals(seed.firstName()))) {
				continue;
			}
			Operateur operateur = new Operateur();
			operateur.setNom(seed.lastName());
			operateur.setPrenom(seed.firstName());
			operateur.setPassword(seed.password());
			operateurs.add(operateurRepository.save(operateur));
		}
	}

	private void seedFactures() throws IOException {
		List<Fournisseur> fournisseurs = fournisseurRepository.findAll();
		List<Produit> products = produitRepository.findAll();
		List<Facture> factures = factureRepository.findAll();
		for (FactureSeed seed : read("seed/factures.json", new TypeReference<List<FactureSeed>>() {
		})) {
			Fournisseur fournisseur = fournisseurs.stream()
					.filter(f -> f.getCode().equals(seed.supplierCode()))
					.findFirst()
					.orElseThrow(() -> new IllegalStateException("Seed supplier not found: " + seed.supplierCode()));
			if (factures.stream().anyMatch(f -> f.getFournisseur().getCode().equals(seed.supplierCode())
					&& f.getDateCreationFacture().equals(date(seed.date()))
					&& f.getMontantFacture() == seed.amount())) {
				continue;
			}
			Facture facture = new Facture();
			facture.setMontantRemise(seed.discount());
			facture.setMontantFacture(seed.amount());
			facture.setDateCreationFacture(date(seed.date()));
			facture.setDateDerniereModificationFacture(date(seed.date()));
			facture.setArchivee(false);
			facture.setFournisseur(fournisseur);
			facture = factureRepository.save(facture);
			for (DetailSeed detailSeed : seed.details()) {
				Produit product = products.stream()
						.filter(p -> p.getCodeProduit().equals(detailSeed.productCode()))
						.findFirst()
						.orElseThrow(() -> new IllegalStateException(
								"Seed product not found: " + detailSeed.productCode()));
				DetailFacture detail = new DetailFacture();
				detail.setQteCommandee(detailSeed.quantity());
				detail.setPrixTotalDetail(detailSeed.total());
				detail.setPourcentageRemise(detailSeed.discountPercentage());
				detail.setMontantRemise(detailSeed.discountAmount());
				detail.setProduit(product);
				detail.setFacture(facture);
				detailFactureRepository.save(detail);
			}
			for (PaymentSeed paymentSeed : seed.payments()) {
				Reglement payment = new Reglement();
				payment.setMontantPaye(paymentSeed.paid());
				payment.setMontantRestant(paymentSeed.remaining());
				payment.setPayee(paymentSeed.paidStatus());
				payment.setDateReglement(date(paymentSeed.date()));
				payment.setFacture(facture);
				reglementRepository.save(payment);
			}
			factures.add(facture);
		}
	}

	private Date date(String value) {
		try {
			return new java.text.SimpleDateFormat("yyyy-MM-dd").parse(value);
		} catch (java.text.ParseException exception) {
			throw new IllegalStateException("Invalid seed date: " + value, exception);
		}
	}

	private void seedStocks() throws IOException {
		List<Stock> stocks = stockRepository.findAll();
		for (StockSeed seed : read("seed/stocks.json", new TypeReference<List<StockSeed>>() {
		})) {
			if (stocks.stream().noneMatch(stock -> stock.getLibelleStock().equals(seed.label()))) {
				Stock stock = new Stock();
				stock.setLibelleStock(seed.label());
				stock.setQte(seed.quantity());
				stock.setQteMin(seed.minimumQuantity());
				stocks.add(stockRepository.save(stock));
			}
		}
	}

	private void seedCategories() throws IOException {
		List<CategorieProduit> categories = categorieProduitRepository.findAll();
		for (CategorySeed seed : read("seed/categories.json", new TypeReference<List<CategorySeed>>() {
		})) {
			if (categories.stream().noneMatch(category -> category.getCodeCategorie().equals(seed.code()))) {
				CategorieProduit category = new CategorieProduit();
				category.setCodeCategorie(seed.code());
				category.setLibelleCategorie(seed.label());
				categories.add(categorieProduitRepository.save(category));
			}
		}
	}

	private void seedProducts() throws IOException {
		List<Stock> stocks = stockRepository.findAll();
		List<CategorieProduit> categories = categorieProduitRepository.findAll();
		List<Produit> products = produitRepository.findAll();

		for (ProductSeed seed : read("seed/products.json", new TypeReference<List<ProductSeed>>() {
		})) {
			if (products.stream().anyMatch(product -> product.getCodeProduit().equals(seed.code()))) {
				continue;
			}
			Stock stock = stocks.stream()
					.filter(candidate -> candidate.getLibelleStock().equals(seed.stockLabel()))
					.findFirst()
					.orElseThrow(() -> new IllegalStateException("Seed stock not found: " + seed.stockLabel()));
			CategorieProduit category = categories.stream()
					.filter(candidate -> candidate.getCodeCategorie().equals(seed.categoryCode()))
					.findFirst()
					.orElseThrow(() -> new IllegalStateException("Seed category not found: " + seed.categoryCode()));

			Produit product = new Produit();
			product.setCodeProduit(seed.code());
			product.setLibelleProduit(seed.label());
			product.setPrix(seed.price());
			product.setDateCreation(new Date());
			product.setDateDerniereModification(new Date());
			product.setStock(stock);
			product.setCategorieProduit(category);
			products.add(produitRepository.save(product));
		}
	}

	private <T> T read(String path, TypeReference<T> type) throws IOException {
		try (InputStream input = new ClassPathResource(path).getInputStream()) {
			return objectMapper.readValue(input, type);
		}
	}

	private record StockSeed(String label, Integer quantity, Integer minimumQuantity) {
	}

	private record CategorySeed(String code, String label) {
	}

	private record ProductSeed(String code, String label, float price, String stockLabel, String categoryCode) {
	}

	private record SecteurSeed(String code, String label) {
	}

	private record FournisseurSeed(String code, String label, String category, String email,
			String startDate, String address, String registrationNumber, List<String> sectorCodes) {
	}

	private record OperateurSeed(String lastName, String firstName, String password) {
	}

	private record FactureSeed(String supplierCode, String date, float amount, float discount,
			List<DetailSeed> details, List<PaymentSeed> payments) {
	}

	private record DetailSeed(String productCode, Integer quantity, float total,
			Integer discountPercentage, float discountAmount) {
	}

	private record PaymentSeed(float paid, float remaining, boolean paidStatus, String date) {
	}
}
