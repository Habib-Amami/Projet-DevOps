package org.example.achat.produit.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.example.achat.produit.entities.CategorieProduit;
import org.example.achat.produit.entities.Produit;
import org.example.achat.produit.exceptions.ProduitNotFoundException;
import org.example.achat.produit.services.IProduitService;
import org.example.achat.stock.entities.Stock;
import org.example.achat.stock.exceptions.StockNotFoundException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProduitRestController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProduitRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IProduitService produitService;

    @Test
    void getProduitsReturnsProduitResponses() throws Exception {
        when(produitService.retrieveAllProduits()).thenReturn(List.of(produit(1L, "PROD-001", "Test product", 12.5f)));

        mockMvc.perform(get("/produits"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idProduit").value(1))
                .andExpect(jsonPath("$[0].codeProduit").value("PROD-001"))
                .andExpect(jsonPath("$[0].libelleProduit").value("Test product"))
                .andExpect(jsonPath("$[0].prix").value(12.5));
    }

    @Test
    void retrieveProduitReturnsProduitResponse() throws Exception {
        when(produitService.retrieveProduit(1L)).thenReturn(produit(1L, "PROD-001", "Test product", 12.5f));

        mockMvc.perform(get("/produits/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idProduit").value(1))
                .andExpect(jsonPath("$.codeProduit").value("PROD-001"))
                .andExpect(jsonPath("$.libelleProduit").value("Test product"));

        verify(produitService).retrieveProduit(1L);
    }

    @Test
    void retrieveMissingProduitReturnsNotFoundProblemDetail() throws Exception {
        when(produitService.retrieveProduit(99L)).thenThrow(new ProduitNotFoundException(99L));

        mockMvc.perform(get("/produits/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Produit not found with id: 99"));
    }

    @Test
    void addProduitReturnsCreatedResponse() throws Exception {
        when(produitService.addProduit(any(Produit.class)))
                .thenReturn(produit(1L, "PROD-001", "Test product", 12.5f));

        mockMvc.perform(post("/produits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(produitJson("PROD-001", "Test product", 12.5f)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idProduit").value(1))
                .andExpect(jsonPath("$.codeProduit").value("PROD-001"))
                .andExpect(jsonPath("$.prix").value(12.5));
    }

    @Test
    void addProduitRejectsInvalidRequest() throws Exception {
        mockMvc.perform(post("/produits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(produitJson("", "", -1)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void modifyProduitUsesPathIdAndReturnsUpdatedResponse() throws Exception {
        when(produitService.updateProduit(any(Produit.class)))
                .thenReturn(produit(1L, "PROD-002", "Updated product", 19.75f));

        mockMvc.perform(put("/produits/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(produitJson("PROD-002", "Updated product", 19.75f)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idProduit").value(1))
                .andExpect(jsonPath("$.libelleProduit").value("Updated product"));

        ArgumentCaptor<Produit> produitCaptor = ArgumentCaptor.forClass(Produit.class);
        verify(produitService).updateProduit(produitCaptor.capture());
        assertThat(produitCaptor.getValue().getIdProduit()).isEqualTo(1L);
        assertThat(produitCaptor.getValue().getCodeProduit()).isEqualTo("PROD-002");
        assertThat(produitCaptor.getValue().getLibelleProduit()).isEqualTo("Updated product");
        assertThat(produitCaptor.getValue().getPrix()).isEqualTo(19.75f);
    }

    @Test
    void removeProduitReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/produits/1"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(produitService).deleteProduit(1L);
    }

    @Test
    void assignProduitToStockReturnsProductWithStockId() throws Exception {
        Produit assigned = produit(1L, "PROD-001", "Test product", 12.5f);
        Stock stock = new Stock();
        stock.setIdStock(2L);
        assigned.setStock(stock);
        org.mockito.Mockito.doNothing().when(produitService).assignProduitToStock(1L, 2L);
        when(produitService.retrieveProduit(1L)).thenReturn(assigned);

        mockMvc.perform(put("/produits/1/stock/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idProduit").value(1))
                .andExpect(jsonPath("$.stockId").value(2));

        verify(produitService).assignProduitToStock(1L, 2L);
        verify(produitService).retrieveProduit(1L);
    }

    @Test
    void assignProduitToMissingStockReturnsNotFoundProblemDetail() throws Exception {
        org.mockito.Mockito.doThrow(new StockNotFoundException(99L))
                .when(produitService).assignProduitToStock(1L, 99L);

        mockMvc.perform(put("/produits/1/stock/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Stock not found with id: 99"));
    }

    private static Produit produit(Long id, String code, String label, float price) {
        Produit produit = new Produit();
        produit.setIdProduit(id);
        produit.setCodeProduit(code);
        produit.setLibelleProduit(label);
        produit.setPrix(price);
        return produit;
    }

    private static String produitJson(String code, String label, float price) {
        return """
                {
                  "codeProduit": "%s",
                  "libelleProduit": "%s",
                  "prix": %s
                }
                """.formatted(code, label, price);
    }
}