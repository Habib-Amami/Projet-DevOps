package org.example.achat.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.stream.Stream;

import com.fasterxml.jackson.databind.json.JsonMapper;
import org.example.achat.facture.controllers.FactureRestController;
import org.example.achat.facture.entities.Facture;
import org.example.achat.facture.services.IFactureService;
import org.example.achat.fournisseurs.controllers.FournisseurRestController;
import org.example.achat.fournisseurs.controllers.SecteurActiviteController;
import org.example.achat.fournisseurs.entities.Fournisseur;
import org.example.achat.fournisseurs.services.IFournisseurService;
import org.example.achat.fournisseurs.services.ISecteurActiviteService;
import org.example.achat.operateur.controllers.OperateurController;
import org.example.achat.operateur.dto.OperateurRequest;
import org.example.achat.operateur.services.IOperateurService;
import org.example.achat.produit.controllers.CategorieProduitController;
import org.example.achat.produit.controllers.ProduitRestController;
import org.example.achat.produit.entities.Produit;
import org.example.achat.produit.services.ICategorieProduitService;
import org.example.achat.produit.services.IProduitService;
import org.example.achat.reglements.controllers.ReglementRestController;
import org.example.achat.reglements.entities.Reglement;
import org.example.achat.reglements.services.IReglementService;
import org.example.achat.stock.controllers.StockRestController;
import org.example.achat.stock.entities.Stock;
import org.example.achat.stock.services.IStockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = {
        StockRestController.class, ProduitRestController.class, CategorieProduitController.class,
        FournisseurRestController.class, SecteurActiviteController.class, OperateurController.class,
        FactureRestController.class, ReglementRestController.class
})
@AutoConfigureMockMvc(addFilters = false)
class RequestDtoContractTest {

    private static final String SUPPLIER = """
            {"code":"SUP-1","libelle":"Supplier","categorieFournisseur":"ORDINAIRE"}
            """;
    private static final String PAYMENT = """
            {"montantPaye":10,"montantRestant":20,"payee":false,"dateReglement":"2026-10-29"}
            """;

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private IStockService stockService;
    @MockitoBean
    private IProduitService produitService;
    @MockitoBean
    private ICategorieProduitService categorieService;
    @MockitoBean
    private IFournisseurService fournisseurService;
    @MockitoBean
    private ISecteurActiviteService secteurService;
    @MockitoBean
    private IOperateurService operateurService;
    @MockitoBean
    private IFactureService factureService;
    @MockitoBean
    private IReglementService reglementService;

    @BeforeEach
    void returnSubmittedEntities() {
        // Return exactly what the controller passes to the service, before persistence.
        when(stockService.addStock(any())).thenAnswer(returnsFirstArg());
        when(stockService.updateStock(any())).thenAnswer(returnsFirstArg());
        when(produitService.addProduit(any())).thenAnswer(returnsFirstArg());
        when(produitService.updateProduit(any())).thenAnswer(returnsFirstArg());
        when(categorieService.addCategorieProduit(any())).thenAnswer(returnsFirstArg());
        when(categorieService.updateCategorieProduit(any())).thenAnswer(returnsFirstArg());
        when(fournisseurService.addFournisseur(any())).thenAnswer(returnsFirstArg());
        when(fournisseurService.updateFournisseur(any())).thenAnswer(returnsFirstArg());
        when(secteurService.addSecteurActivite(any())).thenAnswer(returnsFirstArg());
        when(secteurService.updateSecteurActivite(any())).thenAnswer(returnsFirstArg());
        when(operateurService.addOperateur(any())).thenAnswer(returnsFirstArg());
        when(operateurService.updateOperateur(any())).thenAnswer(returnsFirstArg());
        when(factureService.addFacture(any())).thenAnswer(returnsFirstArg());
        when(reglementService.addReglement(any())).thenAnswer(returnsFirstArg());
        when(reglementService.updateReglement(any())).thenAnswer(returnsFirstArg());
        Facture invoice = new Facture();
        invoice.setIdFacture(10L);
        when(factureService.retrieveFacture(10L)).thenReturn(invoice);
    }

    @ParameterizedTest(name = "POST {0} ignores submitted persistence identifiers")
    @MethodSource("createRequests")
    void createsDoNotBindEntityIds(String path, String payload, String idField, int responseStatus) throws Exception {
        mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().is(responseStatus))
                .andExpect(jsonPath("$." + idField).doesNotExist());
    }

    static Stream<Arguments> createRequests() {
        return Stream.of(
                Arguments.of("/stocks", """
                        {"idStock":999,"libelleStock":"Warehouse","qte":10,"qteMin":2}
                        """, "idStock", 201),
                Arguments.of("/produits", """
                        {"idProduit":999,"codeProduit":"P-1","libelleProduit":"Product","prix":12.5,
                         "dateCreation":"2026-10-29","dateDerniereModification":"2026-10-29"}
                        """, "idProduit", 201),
                Arguments.of("/categories-produits", """
                        {"idCategorieProduit":999,"codeCategorie":"C-1","libelleCategorie":"Category"}
                        """, "idCategorieProduit", 201),
                Arguments.of("/secteurs-activite", """
                        {"idSecteurActivite":999,"codeSecteurActivite":"S-1","libelleSecteurActivite":"Sector"}
                        """, "idSecteurActivite", 201),
                Arguments.of("/operateurs", """
                        {"idOperateur":999,"nom":"Amami","prenom":"Habib","password":"test-secret"}
                        """, "idOperateur", 201),
                Arguments.of("/fournisseurs", """
                        {"idFournisseur":999,"code":"SUP-1","libelle":"Supplier","categorieFournisseur":"ORDINAIRE",
                         "detailFournisseur":{"adresse":"Tunis","matricule":"M-1","email":"supplier@example.com",
                           "dateDebutCollaboration":"2026-10-29"}}
                        """, "idFournisseur", 201),
                Arguments.of("/facture/add-facture", """
                        {"idFacture":999,"montantFacture":50,"montantRemise":5,
                         "dateCreationFacture":"2026-10-29","archivee":false}
                        """, "idFacture", 200),
                Arguments.of("/reglements", withFields(PAYMENT, "\"idReglement\":999,\"factureId\":10"),
                        "idReglement", 201));
    }

    @ParameterizedTest(name = "PUT {0} uses the path identifier")
    @MethodSource("updateRequests")
    void updatesUsePathIds(String path, String payload, String idField) throws Exception {
        mvc.perform(put(path).contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$." + idField).value(12));
    }

    static Stream<Arguments> updateRequests() {
        return Stream.concat(
                createRequests().filter(args -> !args.get()[0].equals("/facture/add-facture"))
                        .filter(args -> !args.get()[0].equals("/fournisseurs"))
                        .map(args -> Arguments.of(args.get()[0] + "/12", args.get()[1], args.get()[2])),
                Stream.of(Arguments.of("/fournisseurs/12", withFields(SUPPLIER, "\"idFournisseur\":999"),
                        "idFournisseur")));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidRequests")
    void invalidRequestsAreRejectedBeforeCallingServices(String description, String path, String payload) throws Exception {
        mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(stockService, produitService, categorieService, fournisseurService,
                secteurService, operateurService, factureService, reglementService);
    }

    static Stream<Arguments> invalidRequests() {
        return Stream.of(
                Arguments.of("Blank stock label", "/stocks", "{\"libelleStock\":\"\",\"qte\":10,\"qteMin\":2}"),
                Arguments.of("Missing stock quantity", "/stocks", "{\"libelleStock\":\"Warehouse\",\"qteMin\":2}"),
                Arguments.of("Negative stock quantity", "/stocks", "{\"libelleStock\":\"Warehouse\",\"qte\":-1,\"qteMin\":2}"),
                Arguments.of("Negative product price", "/produits", "{\"codeProduit\":\"P-1\",\"libelleProduit\":\"Product\",\"prix\":-1}"),
                Arguments.of("Blank category code", "/categories-produits", "{\"codeCategorie\":\"\",\"libelleCategorie\":\"Category\"}"),
                Arguments.of("Blank sector code", "/secteurs-activite", "{\"codeSecteurActivite\":\"\",\"libelleSecteurActivite\":\"Sector\"}"),
                Arguments.of("Missing operator password", "/operateurs", "{\"nom\":\"Amami\",\"prenom\":\"Habib\"}"),
                Arguments.of("Missing supplier details on create", "/fournisseurs", SUPPLIER),
                Arguments.of("Invalid nested supplier email", "/fournisseurs", withFields(SUPPLIER,
                        "\"detailFournisseur\":{\"adresse\":\"Tunis\",\"matricule\":\"M-1\",\"email\":\"invalid\"}")),
                Arguments.of("Blank nested supplier address", "/fournisseurs", withFields(SUPPLIER,
                        "\"detailFournisseur\":{\"adresse\":\"\",\"matricule\":\"M-1\"}")),
                Arguments.of("Blank supplier code", "/fournisseurs", """
                        {"code":"","libelle":"Supplier","categorieFournisseur":"ORDINAIRE",
                         "detailFournisseur":{"adresse":"Tunis","matricule":"M-1"}}
                        """),
                Arguments.of("Negative invoice amount", "/facture/add-facture", "{\"montantFacture\":-1}"),
                Arguments.of("Missing payment invoice", "/reglements", PAYMENT),
                Arguments.of("Invalid payment invoice ID", "/reglements", withFields(PAYMENT, "\"factureId\":-1")),
                Arguments.of("Negative payment amount", "/reglements", """
                        {"montantPaye":-1,"montantRestant":20,"payee":false,"dateReglement":"2026-10-29","factureId":10}
                        """),
                Arguments.of("Missing payment date", "/reglements", "{\"payee\":false,\"factureId\":10}"));
    }

    @Test
    void nestedEntityRelationshipsAreNotBound() throws Exception {
        mvc.perform(post("/stocks").contentType(MediaType.APPLICATION_JSON).content("""
                        {"libelleStock":"Warehouse","qte":10,"qteMin":2,"produits":[{"idProduit":999}]}
                        """))
                .andExpect(status().isCreated());
        ArgumentCaptor<Stock> stocks = ArgumentCaptor.forClass(Stock.class);
        verify(stockService).addStock(stocks.capture());
        assertThat(stocks.getValue().getProduits()).isNull();

        mvc.perform(post("/produits").contentType(MediaType.APPLICATION_JSON).content("""
                        {"codeProduit":"P-1","libelleProduit":"Product","prix":10,
                         "stock":{"idStock":999},"categorieProduit":{"idCategorieProduit":999}}
                        """))
                .andExpect(status().isCreated());
        ArgumentCaptor<Produit> products = ArgumentCaptor.forClass(Produit.class);
        verify(produitService).addProduit(products.capture());
        assertThat(products.getValue().getStock()).isNull();
        assertThat(products.getValue().getCategorieProduit()).isNull();
    }

    @Test
    void supplierDetailsDoNotBindIdentifiersOrBackReferences() throws Exception {
        mvc.perform(post("/fournisseurs").contentType(MediaType.APPLICATION_JSON).content(withFields(SUPPLIER, """
                        "detailFournisseur":{"idDetailFournisseur":999,"adresse":"Tunis","matricule":"M-1",
                          "fournisseur":{"idFournisseur":999}},"factures":[{"idFacture":999}],
                        "secteurActivites":[{"idSecteurActivite":999}]
                        """)))
                .andExpect(status().isCreated());

        ArgumentCaptor<Fournisseur> suppliers = ArgumentCaptor.forClass(Fournisseur.class);
        verify(fournisseurService).addFournisseur(suppliers.capture());
        Fournisseur supplier = suppliers.getValue();
        assertThat(supplier.getDetailFournisseur().getIdDetailFournisseur()).isNull();
        assertThat(supplier.getDetailFournisseur().getFournisseur()).isNull();
        assertThat(supplier.getFactures()).isNull();
        assertThat(supplier.getSecteurActivites()).isNull();
    }

    @Test
    void invoiceRelationshipsAreNotBound() throws Exception {
        mvc.perform(post("/facture/add-facture").contentType(MediaType.APPLICATION_JSON).content("""
                        {"montantFacture":50,"montantRemise":5,"archivee":false,
                         "detailsFacture":[{"idDetailFacture":999}],"fournisseur":{"idFournisseur":999},
                         "reglements":[{"idReglement":999}]}
                        """))
                .andExpect(status().isOk());
        ArgumentCaptor<Facture> invoices = ArgumentCaptor.forClass(Facture.class);
        verify(factureService).addFacture(invoices.capture());
        assertThat(invoices.getValue().getDetailsFacture()).isNull();
        assertThat(invoices.getValue().getFournisseur()).isNull();
        assertThat(invoices.getValue().getReglements()).isNull();
    }

    @Test
    void paymentResolvesOnlyTheExplicitInvoiceId() throws Exception {
        mvc.perform(post("/reglements").contentType(MediaType.APPLICATION_JSON).content(withFields(PAYMENT,
                        "\"factureId\":10,\"facture\":{\"idFacture\":999,\"montantFacture\":999}")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.factureId").value(10));
        ArgumentCaptor<Reglement> payments = ArgumentCaptor.forClass(Reglement.class);
        verify(reglementService).addReglement(payments.capture());
        assertThat(payments.getValue().getFacture()).isSameAs(factureService.retrieveFacture(10L));
    }

    @Test
    void paymentRejectsUnknownInvoice() throws Exception {
        mvc.perform(post("/reglements").contentType(MediaType.APPLICATION_JSON)
                        .content(withFields(PAYMENT, "\"factureId\":999")))
                .andExpect(status().isNotFound());
        verifyNoInteractions(reglementService);
    }

    @Test
    void operatorPasswordIsWriteOnlyAndRedactedFromLogs() throws Exception {
        OperateurRequest request = new OperateurRequest("Amami", "Habib", "test-secret");
        assertThat(request.toString()).doesNotContain("test-secret");
        String json = JsonMapper.builder().build().writeValueAsString(request);
        assertThat(json).contains("Amami").doesNotContain("password", "test-secret");
    }

    @Test
    void paymentUpdateDoesNotRequireOrReplaceTheInvoice() throws Exception {
        mvc.perform(put("/reglements/12").contentType(MediaType.APPLICATION_JSON).content(PAYMENT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idReglement").value(12));
        verifyNoInteractions(factureService);
    }

    private static String withFields(String json, String fields) {
        String trimmed = json.strip();
        return trimmed.substring(0, trimmed.length() - 1) + "," + fields + "}";
    }
}
