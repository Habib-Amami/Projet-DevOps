package org.example.achat.stock.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.example.achat.stock.entities.Stock;
import org.example.achat.stock.exceptions.StockNotFoundException;
import org.example.achat.stock.repositories.StockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StockServiceImplTest {

    @Mock
    private StockRepository stockRepository;

    @InjectMocks
    private StockServiceImpl stockService;

    private Stock stock;

    @BeforeEach
    void setUp() {
        stock = new Stock();
        stock.setIdStock(1L);
        stock.setLibelleStock("Main warehouse");
        stock.setQte(10);
        stock.setQteMin(5);
    }

    @Test
    void retrieveAllStocksReturnsRepositoryResults() {
        when(stockRepository.findAll()).thenReturn(List.of(stock));

        assertThat(stockService.retrieveAllStocks()).containsExactly(stock);

        verify(stockRepository).findAll();
    }

    @Test
    void addStockSavesAndReturnsStock() {
        when(stockRepository.save(stock)).thenReturn(stock);

        assertThat(stockService.addStock(stock)).isSameAs(stock);

        verify(stockRepository).save(stock);
    }

    @Test
    void retrieveStockReturnsExistingStock() {
        when(stockRepository.findById(1L)).thenReturn(Optional.of(stock));

        assertThat(stockService.retrieveStock(1L)).isSameAs(stock);
    }

    @Test
    void retrieveStockThrowsWhenStockDoesNotExist() {
        when(stockRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> stockService.retrieveStock(99L))
                .isInstanceOf(StockNotFoundException.class)
                .hasMessage("Stock not found with id: 99");
    }

    @Test
    void updateStockCopiesEditableFieldsAndSavesExistingStock() {
        Stock changes = new Stock();
        changes.setIdStock(1L);
        changes.setLibelleStock("Updated warehouse");
        changes.setQte(3);
        changes.setQteMin(8);
        when(stockRepository.findById(1L)).thenReturn(Optional.of(stock));
        when(stockRepository.save(stock)).thenReturn(stock);

        Stock result = stockService.updateStock(changes);

        assertThat(result).isSameAs(stock);
        assertThat(stock.getLibelleStock()).isEqualTo("Updated warehouse");
        assertThat(stock.getQte()).isEqualTo(3);
        assertThat(stock.getQteMin()).isEqualTo(8);
        verify(stockRepository).save(stock);
    }

    @Test
    void updateStockThrowsWhenStockDoesNotExist() {
        Stock changes = new Stock();
        changes.setIdStock(99L);
        when(stockRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> stockService.updateStock(changes))
                .isInstanceOf(StockNotFoundException.class)
                .hasMessage("Stock not found with id: 99");

        verify(stockRepository, never()).save(changes);
    }

    @Test
    void deleteStockDeletesExistingStock() {
        when(stockRepository.existsById(1L)).thenReturn(true);

        stockService.deleteStock(1L);

        verify(stockRepository).deleteById(1L);
    }

    @Test
    void deleteStockThrowsWhenStockDoesNotExist() {
        when(stockRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> stockService.deleteStock(99L))
                .isInstanceOf(StockNotFoundException.class);

        verify(stockRepository, never()).deleteById(99L);
    }

    @Test
    void generateLowStockReportIncludesStocksBelowMinimum() {
        stock.setQte(2);
        stock.setQteMin(5);
        when(stockRepository.findStocksBelowMinimumQuantity()).thenReturn(List.of(stock));

        String report = stockService.generateLowStockReport();

        assertThat(report)
                .contains("Main warehouse")
                .contains("2")
                .contains("5");
    }

    @Test
    void generateLowStockReportIsEmptyWhenNoStockIsBelowMinimum() {
        when(stockRepository.findStocksBelowMinimumQuantity()).thenReturn(List.of());

        assertThat(stockService.generateLowStockReport()).isEmpty();
    }
}
