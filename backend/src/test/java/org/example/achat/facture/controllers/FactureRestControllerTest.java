package org.example.achat.facture.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Date;
import java.util.List;

import org.example.achat.facture.entities.Facture;
import org.example.achat.facture.services.IFactureService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(FactureRestController.class)
@AutoConfigureMockMvc(addFilters = false)
class FactureRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IFactureService factureService;

    @Test
    void getFacturesReturnsAllInvoices() throws Exception {
        when(factureService.retrieveAllFactures()).thenReturn(List.of(new Facture()));

        mockMvc.perform(get("/facture/retrieve-all-factures"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        verify(factureService).retrieveAllFactures();
    }

    @Test
    void retrieveFactureReturnsInvoiceById() throws Exception {
        Facture facture = new Facture();
        facture.setIdFacture(7L);
        when(factureService.retrieveFacture(7L)).thenReturn(facture);

        mockMvc.perform(get("/facture/retrieve-facture/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idFacture").value(7));

        verify(factureService).retrieveFacture(7L);
    }

    @Test
    void getFactureByFournisseurReturnsServiceResult() throws Exception {
        when(factureService.getFacturesByFournisseur(7L)).thenReturn(List.of(new Facture()));

        mockMvc.perform(get("/facture/getFactureByFournisseur/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        verify(factureService).getFacturesByFournisseur(7L);
    }

    @Test
    void assignOperateurToFactureDelegatesBothIds() throws Exception {
        mockMvc.perform(put("/facture/assignOperateurToFacture/3/7"))
                .andExpect(status().isOk());

        verify(factureService).assignOperateurToFacture(3L, 7L);
    }

    @Test
    void pourcentageRecouvrementReturnsServiceResult() throws Exception {
        when(factureService.pourcentageRecouvrement(any(Date.class), any(Date.class))).thenReturn(62.5f);

        mockMvc.perform(get("/facture/pourcentageRecouvrement/2026-01-01/2026-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(62.5));

        verify(factureService).pourcentageRecouvrement(any(Date.class), any(Date.class));
    }

    @Test
    void pourcentageRecouvrementReturnsZeroWhenServiceFails() throws Exception {
        when(factureService.pourcentageRecouvrement(any(Date.class), any(Date.class)))
                .thenThrow(new IllegalStateException("Revenue query failed"));

        mockMvc.perform(get("/facture/pourcentageRecouvrement/2026-01-01/2026-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(0));
    }
}
