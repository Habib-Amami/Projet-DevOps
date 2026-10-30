package org.example.achat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

class ApiWorkflowIntegrationTest extends MySqlIntegrationSupport {

    private final ObjectMapper json = new ObjectMapper();

    @Test
    void stockAndProductLifecyclePersistsAcrossRequests() throws Exception {
        long stockId = create("/stocks", """
                {"libelleStock":"Warehouse","qte":10,"qteMin":2}
                """, "idStock");
        long productId = create("/produits", """
                {"codeProduit":"P-1","libelleProduit":"Product","prix":25}
                """, "idProduit");

        mvc.perform(put("/produits/{id}/stock/{stockId}", productId, stockId))
                .andExpect(status().isOk()).andExpect(jsonPath("$.stockId").value(stockId));
        mvc.perform(get("/produits/{id}", productId))
                .andExpect(status().isOk()).andExpect(jsonPath("$.stockId").value(stockId));
        mvc.perform(put("/produits/{id}", productId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codeProduit\":\"P-2\",\"libelleProduit\":\"Updated\",\"prix\":30}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.prix").value(30))
                .andExpect(jsonPath("$.stockId").value(stockId));
        mvc.perform(put("/stocks/{id}", stockId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"libelleStock\":\"Updated warehouse\",\"qte\":5,\"qteMin\":3}"))
                .andExpect(status().isOk());
        mvc.perform(get("/stocks/{id}", stockId)).andExpect(status().isOk())
                .andExpect(jsonPath("$.libelleStock").value("Updated warehouse"))
                .andExpect(jsonPath("$.qte").value(5));

        mvc.perform(delete("/produits/{id}", productId)).andExpect(status().isNoContent());
        mvc.perform(delete("/stocks/{id}", stockId)).andExpect(status().isNoContent());
        mvc.perform(get("/produits/{id}", productId)).andExpect(status().isNotFound());
        mvc.perform(get("/stocks/{id}", stockId)).andExpect(status().isNotFound());
        assertThat(products.existsById(productId)).isFalse();
        assertThat(stocks.existsById(stockId)).isFalse();
    }

    @Test
    void supplierLifecyclePreservesDetailsAndSectorOnUpdate() throws Exception {
        long sectorId = create("/secteurs-activite", """
                {"codeSecteurActivite":"S-1","libelleSecteurActivite":"Sector"}
                """, "idSecteurActivite");
        long supplierId = create("/fournisseurs", """
                {"code":"SUP-1","libelle":"Supplier","categorieFournisseur":"ORDINAIRE",
                 "detailFournisseur":{"adresse":"Tunis","matricule":"SUP-1","email":"supplier@example.com"}}
                """, "idFournisseur");
        long detailId = jdbc.queryForObject("""
                SELECT detail_fournisseur_id_detail_fournisseur FROM fournisseur WHERE id_fournisseur = ?
                """, Long.class, supplierId);

        mvc.perform(put("/fournisseurs/{id}/secteurs-activite/{sectorId}", supplierId, sectorId))
                .andExpect(status().isOk()).andExpect(jsonPath("$.secteurActiviteIds[0]").value(sectorId));
        mvc.perform(put("/fournisseurs/{id}", supplierId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"SUP-2\",\"libelle\":\"Updated\",\"categorieFournisseur\":\"CONVENTIONNE\"}"))
                .andExpect(status().isOk());
        mvc.perform(get("/fournisseurs/{id}", supplierId))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("SUP-2"))
                .andExpect(jsonPath("$.detailFournisseurId").value(detailId))
                .andExpect(jsonPath("$.secteurActiviteIds[0]").value(sectorId));

        mvc.perform(delete("/fournisseurs/{id}", supplierId)).andExpect(status().isNoContent());
        mvc.perform(get("/fournisseurs/{id}", supplierId)).andExpect(status().isNotFound());
        mvc.perform(delete("/secteurs-activite/{id}", sectorId)).andExpect(status().isNoContent());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM detail_fournisseur", Long.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM fournisseur_secteur_activites", Long.class)).isZero();
    }

    @Test
    void paymentLifecycleKeepsInvoiceAndArchiveRemovesRevenue() throws Exception {
        MvcResult invoiceResponse = mvc.perform(post("/facture/add-facture")
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"montantFacture":100,"montantRemise":0,"archivee":false,
                                 "dateCreationFacture":"2026-01-10"}
                                """))
                .andExpect(status().isOk()).andReturn();
        long invoiceId = id(invoiceResponse, "idFacture");
        long paymentId = create("/reglements", """
                {"montantPaye":25,"montantRestant":75,"payee":false,
                 "dateReglement":"2026-01-10","factureId":%d}
                """.formatted(invoiceId), "idReglement");

        mvc.perform(put("/reglements/{id}", paymentId).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"montantPaye":100,"montantRestant":0,"payee":true,"dateReglement":"2026-01-11"}
                                """))
                .andExpect(status().isOk()).andExpect(jsonPath("$.factureId").value(invoiceId));
        mvc.perform(get("/reglements/{id}", paymentId)).andExpect(status().isOk())
                .andExpect(jsonPath("$.montantPaye").value(100)).andExpect(jsonPath("$.payee").value(true));
        mvc.perform(get("/reglements/facture/{id}", invoiceId)).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idReglement").value(paymentId));
        assertRevenue(100);

        mvc.perform(put("/facture/cancel-facture/{id}", invoiceId)).andExpect(status().isOk());
        assertRevenue(0);
        // Archiving an invoice must not delete its payment history.
        assertThat(payments.existsById(paymentId)).isTrue();
        mvc.perform(delete("/reglements/{id}", paymentId)).andExpect(status().isNoContent());
        mvc.perform(get("/reglements/{id}", paymentId)).andExpect(status().isNotFound());
        assertThat(invoices.existsById(invoiceId)).isTrue();
    }

    @Test
    void nonexistentInvoiceCannotCreateOrphanPayment() throws Exception {
        mvc.perform(post("/reglements").contentType(MediaType.APPLICATION_JSON).content("""
                        {"montantPaye":25,"montantRestant":75,"payee":false,
                         "dateReglement":"2026-01-10","factureId":9223372036854775807}
                        """))
                .andExpect(status().isNotFound());
        assertThat(payments.count()).isZero();
    }

    @ParameterizedTest
    @CsvSource({
            "/categories-produits,idCategorieProduit,codeCategorie,libelleCategorie",
            "/secteurs-activite,idSecteurActivite,codeSecteurActivite,libelleSecteurActivite"
    })
    void categoryAndSectorCrudUsesRealPersistence(String path, String idField, String code, String label)
            throws Exception {
        long id = create(path, "{\"%s\":\"CODE-1\",\"%s\":\"Original\"}".formatted(code, label), idField);
        mvc.perform(get(path + "/{id}", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$." + code).value("CODE-1"));
        mvc.perform(put(path + "/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"%s\":\"CODE-2\",\"%s\":\"Updated\"}".formatted(code, label)))
                .andExpect(status().isOk());
        mvc.perform(get(path + "/{id}", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$." + label).value("Updated"));
        mvc.perform(delete(path + "/{id}", id)).andExpect(status().isNoContent());
        mvc.perform(get(path + "/{id}", id)).andExpect(status().isNotFound());
    }

    private long create(String path, String payload, String idField) throws Exception {
        MvcResult result = mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isCreated()).andReturn();
        return id(result, idField);
    }

    private long id(MvcResult result, String field) throws Exception {
        return json.readTree(result.getResponse().getContentAsString()).required(field).longValue();
    }

    private void assertRevenue(int expected) throws Exception {
        mvc.perform(get("/reglements/revenue").param("startDate", "2026-01-01").param("endDate", "2026-01-31"))
                .andExpect(status().isOk()).andExpect(jsonPath("$").value(expected));
    }
}
