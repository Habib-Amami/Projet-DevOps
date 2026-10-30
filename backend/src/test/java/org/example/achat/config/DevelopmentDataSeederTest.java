package org.example.achat.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.example.achat.facture.entities.DetailFacture;
import org.example.achat.facture.entities.Facture;
import org.example.achat.facture.repositories.DetailFactureRepository;
import org.example.achat.facture.repositories.FactureRepository;
import org.example.achat.fournisseurs.entities.DetailFournisseur;
import org.example.achat.fournisseurs.entities.Fournisseur;
import org.example.achat.fournisseurs.entities.SecteurActivite;
import org.example.achat.fournisseurs.repositories.DetailFournisseurRepository;
import org.example.achat.fournisseurs.repositories.FournisseurRepository;
import org.example.achat.fournisseurs.repositories.SecteurActiviteRepository;
import org.example.achat.operateur.entities.Operateur;
import org.example.achat.operateur.repositories.OperateurRepository;
import org.example.achat.produit.entities.CategorieProduit;
import org.example.achat.produit.entities.Produit;
import org.example.achat.produit.repositories.CategorieProduitRepository;
import org.example.achat.produit.repositories.ProduitRepository;
import org.example.achat.reglements.entities.Reglement;
import org.example.achat.reglements.repositories.ReglementRepository;
import org.example.achat.stock.entities.Stock;
import org.example.achat.stock.repositories.StockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;

@ExtendWith(MockitoExtension.class)
class DevelopmentDataSeederTest {

    @Mock
    private StockRepository stockRepository;
    @Mock
    private CategorieProduitRepository categorieProduitRepository;
    @Mock
    private ProduitRepository produitRepository;
    @Mock
    private SecteurActiviteRepository secteurActiviteRepository;
    @Mock
    private DetailFournisseurRepository detailFournisseurRepository;
    @Mock
    private FournisseurRepository fournisseurRepository;
    @Mock
    private OperateurRepository operateurRepository;
    @Mock
    private FactureRepository factureRepository;
    @Mock
    private DetailFactureRepository detailFactureRepository;
    @Mock
    private ReglementRepository reglementRepository;

    private final List<Stock> stocks = new ArrayList<>();
    private final List<CategorieProduit> categories = new ArrayList<>();
    private final List<Produit> products = new ArrayList<>();
    private final List<SecteurActivite> sectors = new ArrayList<>();
    private final List<Fournisseur> suppliers = new ArrayList<>();
    private final List<Operateur> operators = new ArrayList<>();
    private final List<Facture> invoices = new ArrayList<>();
    private DevelopmentDataSeeder seeder;

    @BeforeEach
    void setUp() {
        when(stockRepository.findAll()).thenReturn(stocks);
        when(stockRepository.save(any(Stock.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(categorieProduitRepository.findAll()).thenReturn(categories);
        when(categorieProduitRepository.save(any(CategorieProduit.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(produitRepository.findAll()).thenReturn(products);
        when(produitRepository.save(any(Produit.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(secteurActiviteRepository.findAll()).thenReturn(sectors);
        when(secteurActiviteRepository.save(any(SecteurActivite.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(detailFournisseurRepository.save(any(DetailFournisseur.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(fournisseurRepository.findAll()).thenReturn(suppliers);
        when(fournisseurRepository.save(any(Fournisseur.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(operateurRepository.findAll()).thenReturn(operators);
        when(operateurRepository.save(any(Operateur.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(factureRepository.findAll()).thenReturn(invoices);
        when(factureRepository.save(any(Facture.class))).thenAnswer(invocation -> invocation.getArgument(0));

        seeder = new DevelopmentDataSeeder(
                stockRepository,
                categorieProduitRepository,
                produitRepository,
                secteurActiviteRepository,
                detailFournisseurRepository,
                fournisseurRepository,
                operateurRepository,
                factureRepository,
                detailFactureRepository,
                reglementRepository);
    }

    @Test
    void seedsEveryDomainAndSkipsExistingRecordsOnSubsequentRuns() throws Exception {
        var args = new DefaultApplicationArguments();

        seeder.run(args);

        assertThat(stocks).hasSize(2);
        assertThat(categories).hasSize(2);
        assertThat(products).hasSize(2);
        assertThat(sectors).hasSize(3);
        assertThat(suppliers).hasSize(2);
        assertThat(operators).hasSize(2);
        assertThat(invoices).hasSize(2);
        assertThat(invoices).allSatisfy(invoice -> assertThat(invoice.getFournisseur()).isNotNull());

        verify(detailFactureRepository, times(3)).save(any(DetailFacture.class));
        verify(reglementRepository, times(2)).save(any(Reglement.class));
        verify(detailFournisseurRepository, times(2)).save(any());

        seeder.run(args);

        assertThat(stocks).hasSize(2);
        assertThat(categories).hasSize(2);
        assertThat(products).hasSize(2);
        assertThat(sectors).hasSize(3);
        assertThat(suppliers).hasSize(2);
        assertThat(operators).hasSize(2);
        assertThat(invoices).hasSize(2);
        verify(factureRepository, times(2)).save(any(Facture.class));
    }
}
