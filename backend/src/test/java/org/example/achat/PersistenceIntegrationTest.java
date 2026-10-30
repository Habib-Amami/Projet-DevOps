package org.example.achat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashSet;
import java.util.List;

import org.example.achat.facture.entities.DetailFacture;
import org.example.achat.facture.repositories.DetailFactureRepository;
import org.example.achat.fournisseurs.entities.CategorieFournisseur;
import org.example.achat.fournisseurs.entities.DetailFournisseur;
import org.example.achat.fournisseurs.entities.Fournisseur;
import org.example.achat.fournisseurs.entities.SecteurActivite;
import org.example.achat.fournisseurs.repositories.FournisseurRepository;
import org.example.achat.fournisseurs.repositories.SecteurActiviteRepository;
import org.example.achat.produit.entities.CategorieProduit;
import org.example.achat.produit.repositories.CategorieProduitRepository;
import org.example.achat.produit.services.IProduitService;
import org.example.achat.stock.services.IStockService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

class PersistenceIntegrationTest extends MySqlIntegrationSupport {

    @Autowired private CategorieProduitRepository categories;
    @Autowired private FournisseurRepository suppliers;
    @Autowired private SecteurActiviteRepository sectors;
    @Autowired private DetailFactureRepository invoiceLines;
    @Autowired private IStockService stockService;
    @Autowired private IProduitService productService;

    @Test
    void productStockAndCategoryRelationshipsAreCommitted() {
        var stock = stock("Warehouse", 10, 2);
        CategorieProduit category = new CategorieProduit();
        category.setCodeCategorie("CAT-1");
        category.setLibelleCategorie("Category");
        category = categories.saveAndFlush(category);
        var product = product(stock);
        product.setCategorieProduit(category);
        products.saveAndFlush(product);

        // A separate JDBC read checks committed rows, not Hibernate's first-level cache.
        var row = jdbc.queryForMap("""
                SELECT stock_id_stock, categorie_produit_id_categorie_produit
                FROM produit WHERE id_produit = ?
                """, product.getIdProduit());
        assertThat(((Number) row.get("stock_id_stock")).longValue()).isEqualTo(stock.getIdStock());
        assertThat(((Number) row.get("categorie_produit_id_categorie_produit")).longValue())
                .isEqualTo(category.getIdCategorieProduit());
    }

    @Test
    void supplierDetailsCascadeAndSectorsAreReadableAfterSessionCloses() {
        SecteurActivite sector = new SecteurActivite();
        sector.setCodeSecteurActivite("SECTOR-1");
        sector.setLibelleSecteurActivite("Business sector");
        sector = sectors.saveAndFlush(sector);
        DetailFournisseur detail = new DetailFournisseur();
        detail.setAdresse("Tunis");
        detail.setMatricule("SUPPLIER-1");
        detail.setEmail("supplier@example.com");
        Fournisseur supplier = new Fournisseur();
        supplier.setCode("SUP-1");
        supplier.setLibelle("Supplier");
        supplier.setCategorieFournisseur(CategorieFournisseur.ORDINAIRE);
        supplier.setDetailFournisseur(detail);
        supplier.setSecteurActivites(new HashSet<>(List.of(sector)));
        supplier = suppliers.saveAndFlush(supplier);

        Fournisseur reloaded = suppliers.findById(supplier.getIdFournisseur()).orElseThrow();
        // The repository graph fetches sectors, while the detail may remain a proxy.
        // Read its committed row separately instead of relying on an open session.
        assertThat(jdbc.queryForObject("""
                SELECT matricule FROM detail_fournisseur WHERE id_detail_fournisseur = ?
                """, String.class, reloaded.getDetailFournisseur().getIdDetailFournisseur()))
                .isEqualTo("SUPPLIER-1");
        assertThat(reloaded.getSecteurActivites()).extracting(SecteurActivite::getIdSecteurActivite)
                .containsExactly(sector.getIdSecteurActivite());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM detail_fournisseur", Long.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM fournisseur_secteur_activites", Long.class))
                .isEqualTo(1);
    }

    @Test
    void invoiceDetailsAndProductsCanBeReadOutsidePersistenceSession() {
        var invoice = invoice(50, "2026-01-10", false);
        var product = product(null);
        DetailFacture line = new DetailFacture();
        line.setFacture(invoice);
        line.setProduit(product);
        line.setQteCommandee(2);
        line.setPrixTotalDetail(50);
        invoiceLines.saveAndFlush(line);

        var reloaded = invoices.findById(invoice.getIdFacture()).orElseThrow();
        assertThat(reloaded.getDetailsFacture()).singleElement().satisfies(detail -> {
            assertThat(detail.getQteCommandee()).isEqualTo(2);
            assertThat(detail.getProduit().getCodeProduit()).isEqualTo("PRODUCT-1");
        });
    }

    @Test
    void stockDeletionIsRejectedUntilItsProductIsMoved() {
        var original = stock("Original", 10, 2);
        var destination = stock("Destination", 10, 2);
        var product = product(original);

        assertThatThrownBy(() -> stockService.deleteStock(original.getIdStock()))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasStackTraceContaining("fk_produit_stock");
        assertThat(stocks.existsById(original.getIdStock())).isTrue();
        assertThat(products.existsById(product.getIdProduit())).isTrue();

        productService.assignProduitToStock(product.getIdProduit(), destination.getIdStock());
        stockService.deleteStock(original.getIdStock());

        assertThat(stocks.existsById(original.getIdStock())).isFalse();
        assertThat(jdbc.queryForObject("SELECT stock_id_stock FROM produit WHERE id_produit = ?",
                Long.class, product.getIdProduit())).isEqualTo(destination.getIdStock());
    }

    @Test
    void productDeletionPreservesHistoricalInvoiceLines() {
        var invoice = invoice(25, "2026-01-10", false);
        var product = product(null);
        DetailFacture line = new DetailFacture();
        line.setFacture(invoice);
        line.setProduit(product);
        line.setQteCommandee(1);
        line.setPrixTotalDetail(25);
        line = invoiceLines.saveAndFlush(line);

        assertThatThrownBy(() -> productService.deleteProduit(product.getIdProduit()))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasStackTraceContaining("fk_detail_facture_produit");

        assertThat(products.existsById(product.getIdProduit())).isTrue();
        assertThat(invoiceLines.existsById(line.getIdDetailFacture())).isTrue();
        assertThat(invoices.existsById(invoice.getIdFacture())).isTrue();
    }

    @Test
    void unreferencedProductAndStockCanBeDeleted() {
        var stock = stock("Unused warehouse", 10, 2);
        var product = product(stock);

        productService.deleteProduit(product.getIdProduit());
        stockService.deleteStock(stock.getIdStock());

        assertThat(products.existsById(product.getIdProduit())).isFalse();
        assertThat(stocks.existsById(stock.getIdStock())).isFalse();
    }
}
