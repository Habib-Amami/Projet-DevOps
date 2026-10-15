package org.example.achat.produit.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.example.achat.produit.entities.Produit;
import org.example.achat.produit.exceptions.ProduitNotFoundException;
import org.example.achat.produit.repositories.CategorieProduitRepository;
import org.example.achat.produit.repositories.ProduitRepository;
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
class ProduitServiceImplTest {

    @Mock
    private ProduitRepository produitRepository;

    @Mock
    private StockRepository stockRepository;

    @Mock
    private CategorieProduitRepository categorieProduitRepository;

    @InjectMocks
    private ProduitServiceImpl produitService;

    private Produit produit;

    @BeforeEach
    void setUp() {
        produit = new Produit();
        produit.setIdProduit(1L);
        produit.setCodeProduit("PROD-001");
        produit.setLibelleProduit("Test product");
        produit.setPrix(12.5f);
    }

    @Test
    void retrieveAllProduitsReturnsRepositoryResults() {
        when(produitRepository.findAll()).thenReturn(List.of(produit));

        assertThat(produitService.retrieveAllProduits()).containsExactly(produit);

        verify(produitRepository).findAll();
    }

    @Test
    void addProduitSavesAndReturnsProduit() {
        when(produitRepository.save(produit)).thenReturn(produit);

        assertThat(produitService.addProduit(produit)).isSameAs(produit);

        verify(produitRepository).save(produit);
    }

    @Test
    void retrieveProduitReturnsExistingProduit() {
        when(produitRepository.findById(1L)).thenReturn(Optional.of(produit));

        assertThat(produitService.retrieveProduit(1L)).isSameAs(produit);
    }

    @Test
    void retrieveProduitThrowsWhenProduitDoesNotExist() {
        when(produitRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> produitService.retrieveProduit(99L))
                .isInstanceOf(ProduitNotFoundException.class)
                .hasMessage("Produit not found with id: 99");
    }

    @Test
    void updateProduitCopiesEditableFieldsAndSavesExistingProduit() {
        Produit changes = new Produit();
        changes.setIdProduit(1L);
        changes.setCodeProduit("PROD-002");
        changes.setLibelleProduit("Updated product");
        changes.setPrix(19.75f);
        Date modificationDate = new Date();
        changes.setDateDerniereModification(modificationDate);
        when(produitRepository.findById(1L)).thenReturn(Optional.of(produit));
        when(produitRepository.save(produit)).thenReturn(produit);

        Produit result = produitService.updateProduit(changes);

        assertThat(result).isSameAs(produit);
        assertThat(produit.getCodeProduit()).isEqualTo("PROD-002");
        assertThat(produit.getLibelleProduit()).isEqualTo("Updated product");
        assertThat(produit.getPrix()).isEqualTo(19.75f);
        assertThat(produit.getDateDerniereModification()).isEqualTo(modificationDate);
        verify(produitRepository).save(produit);
    }

    @Test
    void updateProduitThrowsWhenProduitDoesNotExist() {
        Produit changes = new Produit();
        changes.setIdProduit(99L);
        when(produitRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> produitService.updateProduit(changes))
                .isInstanceOf(ProduitNotFoundException.class)
                .hasMessage("Produit not found with id: 99");

        verify(produitRepository, never()).save(changes);
    }

    @Test
    void deleteProduitDeletesExistingProduit() {
        when(produitRepository.existsById(1L)).thenReturn(true);

        produitService.deleteProduit(1L);

        verify(produitRepository).deleteById(1L);
    }

    @Test
    void deleteProduitThrowsWhenProduitDoesNotExist() {
        when(produitRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> produitService.deleteProduit(99L))
                .isInstanceOf(ProduitNotFoundException.class)
                .hasMessage("Produit not found with id: 99");

        verify(produitRepository, never()).deleteById(99L);
    }

    @Test
    void assignProduitToStockSavesProduitWithStock() {
        Stock stock = new Stock();
        stock.setIdStock(2L);
        when(produitRepository.findById(1L)).thenReturn(Optional.of(produit));
        when(stockRepository.findById(2L)).thenReturn(Optional.of(stock));
        when(produitRepository.save(produit)).thenReturn(produit);

        produitService.assignProduitToStock(1L, 2L);

        assertThat(produit.getStock()).isSameAs(stock);
        verify(produitRepository).save(produit);
    }

    @Test
    void assignProduitToStockThrowsWhenProduitDoesNotExist() {
        when(produitRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> produitService.assignProduitToStock(99L, 2L))
                .isInstanceOf(ProduitNotFoundException.class)
                .hasMessage("Produit not found with id: 99");

        verify(stockRepository, never()).findById(2L);
        verify(produitRepository, never()).save(produit);
    }

    @Test
    void assignProduitToStockThrowsWhenStockDoesNotExist() {
        when(produitRepository.findById(1L)).thenReturn(Optional.of(produit));
        when(stockRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> produitService.assignProduitToStock(1L, 99L))
                .isInstanceOf(StockNotFoundException.class)
                .hasMessage("Stock not found with id: 99");

        verify(produitRepository, never()).save(produit);
    }
}