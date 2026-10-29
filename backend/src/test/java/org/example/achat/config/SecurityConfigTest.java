package org.example.achat.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.example.achat.stock.controllers.StockRestController;
import org.example.achat.stock.entities.Stock;
import org.example.achat.stock.services.IStockService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(StockRestController.class)
@Import(SecurityConfig.class)
class SecurityConfigTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private IStockService stockService;

    @Test
    void anonymousReadDoesNotRequireBrowserCredentials() throws Exception {
        when(stockService.retrieveAllStocks()).thenReturn(List.of());

        mvc.perform(get("/stocks"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist(HttpHeaders.WWW_AUTHENTICATE))
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
    }

    @Test
    void anonymousJsonWriteNeedsNoCsrfTokenAndCreatesNoSession() throws Exception {
        when(stockService.addStock(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = mvc.perform(post("/stocks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"libelleStock\":\"Warehouse\",\"qte\":10,\"qteMin\":2}"))
                .andExpect(status().isCreated())
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE))
                .andReturn();

        assertThat(result.getRequest().getSession(false)).isNull();
        verify(stockService).addStock(any(Stock.class));
    }

    @Test
    void browserFormCannotBindAJsonRequestDto() throws Exception {
        mvc.perform(post("/stocks")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .content("libelleStock=Warehouse&qte=10&qteMin=2"))
                .andExpect(status().isUnsupportedMediaType());

        verifyNoInteractions(stockService);
    }
}
