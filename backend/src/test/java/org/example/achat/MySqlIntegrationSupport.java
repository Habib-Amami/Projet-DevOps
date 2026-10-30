package org.example.achat;

import java.sql.Date;

import org.example.achat.facture.entities.Facture;
import org.example.achat.facture.repositories.FactureRepository;
import org.example.achat.produit.entities.Produit;
import org.example.achat.produit.repositories.ProduitRepository;
import org.example.achat.reglements.entities.Reglement;
import org.example.achat.reglements.repositories.ReglementRepository;
import org.example.achat.stock.entities.Stock;
import org.example.achat.stock.repositories.StockRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Shared Spring context and real MySQL service; no Mockito replacements.
 * Tests deliberately have no outer transaction, so commits, lazy loading, and
 * foreign-key failures behave as they do across real application requests.
 */
@SpringBootTest(properties = "spring.config.location=classpath:/application-integration.properties")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
abstract class MySqlIntegrationSupport {

    @Autowired protected MockMvc mvc;
    @Autowired protected JdbcTemplate jdbc;
    @Autowired protected FactureRepository invoices;
    @Autowired protected ReglementRepository payments;
    @Autowired protected StockRepository stocks;
    @Autowired protected ProduitRepository products;

    @BeforeEach
    @AfterEach
    void clearTestDatabase() {
        // Child tables first. Keep Flyway history and enforce all foreign keys.
        // This datasource is the Testcontainers service, never the local/cluster DB.
        for (String table : new String[] {
                "operateur_factures", "detail_facture", "reglement", "facture",
                "fournisseur_secteur_activites", "fournisseur", "detail_fournisseur",
                "produit", "categorie_produit", "stock", "secteur_activite", "operateur"}) {
            jdbc.update("DELETE FROM " + table);
        }
    }

    protected Facture invoice(float total, String creationDate, boolean archived) {
        Facture invoice = new Facture();
        invoice.setMontantFacture(total);
        invoice.setDateCreationFacture(Date.valueOf(creationDate));
        invoice.setArchivee(archived);
        return invoices.saveAndFlush(invoice);
    }

    protected Reglement payment(Facture invoice, float amount, String paymentDate) {
        Reglement payment = new Reglement();
        payment.setFacture(invoice);
        payment.setMontantPaye(amount);
        payment.setMontantRestant(0);
        payment.setPayee(true);
        payment.setDateReglement(Date.valueOf(paymentDate));
        return payments.saveAndFlush(payment);
    }

    protected Stock stock(String label, int quantity, int minimum) {
        Stock stock = new Stock();
        stock.setLibelleStock(label);
        stock.setQte(quantity);
        stock.setQteMin(minimum);
        return stocks.saveAndFlush(stock);
    }

    protected Produit product(Stock stock) {
        Produit product = new Produit();
        product.setCodeProduit("PRODUCT-1");
        product.setLibelleProduit("Integration product");
        product.setPrix(25);
        product.setStock(stock);
        return products.saveAndFlush(product);
    }
}
