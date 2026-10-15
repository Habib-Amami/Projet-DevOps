package org.example.achat.stock.controllers;

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
import org.example.achat.stock.entities.Stock;
import org.example.achat.stock.exceptions.StockNotFoundException;
import org.example.achat.stock.services.IStockService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;

@WebMvcTest(StockRestController.class)
@AutoConfigureMockMvc(addFilters = false)
class StockRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IStockService stockService;

    @Test
    void getStocksReturnsStockResponses() throws Exception {
        Stock stock = stock(1L, "Main warehouse", 10, 5);
        when(stockService.retrieveAllStocks()).thenReturn(List.of(stock));

        mockMvc.perform(get("/stocks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idStock").value(1))
                .andExpect(jsonPath("$[0].libelleStock").value("Main warehouse"))
                .andExpect(jsonPath("$[0].qte").value(10))
                .andExpect(jsonPath("$[0].qteMin").value(5));
    }

    @Test
    void retrieveStockReturnsStockResponse() throws Exception {
        when(stockService.retrieveStock(1L)).thenReturn(stock(1L, "Main warehouse", 10, 5));

        mockMvc.perform(get("/stocks/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idStock").value(1))
                .andExpect(jsonPath("$.libelleStock").value("Main warehouse"));

        verify(stockService).retrieveStock(1L);
    }

    @Test
    void retrieveMissingStockReturnsNotFoundProblemDetail() throws Exception {
        when(stockService.retrieveStock(99L)).thenThrow(new StockNotFoundException(99L));

        mockMvc.perform(get("/stocks/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Stock not found with id: 99"));
    }

    @Test
    void addStockReturnsCreatedResponse() throws Exception {
        Stock saved = stock(1L, "Main warehouse", 10, 5);
        when(stockService.addStock(any(Stock.class))).thenReturn(saved);

        mockMvc.perform(post("/stocks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(stockJson("Main warehouse", 10, 5)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idStock").value(1))
                .andExpect(jsonPath("$.qte").value(10));
    }

    @Test
    void addStockRejectsInvalidRequest() throws Exception {
        mockMvc.perform(post("/stocks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(stockJson("", -1, 5)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void modifyStockUsesPathIdAndReturnsUpdatedResponse() throws Exception {
        Stock updated = stock(1L, "Updated warehouse", 3, 8);
        when(stockService.updateStock(any(Stock.class))).thenReturn(updated);

        mockMvc.perform(put("/stocks/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(stockJson("Updated warehouse", 3, 8)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.libelleStock").value("Updated warehouse"));

        ArgumentCaptor<Stock> stockCaptor = ArgumentCaptor.forClass(Stock.class);
        verify(stockService).updateStock(stockCaptor.capture());
        Stock request = stockCaptor.getValue();
        org.assertj.core.api.Assertions.assertThat(request.getIdStock()).isEqualTo(1L);
        org.assertj.core.api.Assertions.assertThat(request.getLibelleStock()).isEqualTo("Updated warehouse");
        org.assertj.core.api.Assertions.assertThat(request.getQte()).isEqualTo(3);
        org.assertj.core.api.Assertions.assertThat(request.getQteMin()).isEqualTo(8);
    }

    @Test
    void removeStockReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/stocks/1"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(stockService).deleteStock(1L);
    }

    private static Stock stock(Long id, String label, int quantity, int minimum) {
        Stock stock = stockWithId(id, label, quantity, minimum);
        return stock;
    }

    private static Stock stockWithId(Long id, String label, int quantity, int minimum) {
        Stock stock = new Stock();
        stock.setIdStock(id);
        stock.setLibelleStock(label);
        stock.setQte(quantity);
        stock.setQteMin(minimum);
        return stock;
    }

    private static String stockJson(String label, int quantity, int minimum) {
        return """
                {
                  "libelleStock": "%s",
                  "qte": %d,
                  "qteMin": %d
                }
                """.formatted(label, quantity, minimum);
    }
}
