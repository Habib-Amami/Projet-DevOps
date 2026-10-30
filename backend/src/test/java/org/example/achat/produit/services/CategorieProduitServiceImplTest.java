package org.example.achat.produit.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.example.achat.produit.entities.CategorieProduit;
import org.example.achat.produit.exceptions.CategorieProduitNotFoundException;
import org.example.achat.produit.repositories.CategorieProduitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CategorieProduitServiceImplTest {

    @Mock private CategorieProduitRepository repository;
    @InjectMocks private CategorieProduitServiceImpl service;
    private CategorieProduit entity;

    @BeforeEach
    void createEntity() {
        entity = new CategorieProduit();
        entity.setIdCategorieProduit(1L);
        entity.setCodeCategorie("CAT-1");
        entity.setLibelleCategorie("Original label");
    }

    @Test
    void listsExistingRecords() {
        when(repository.findAll()).thenReturn(List.of(entity));
        assertThat(service.retrieveAllCategorieProduits()).containsExactly(entity);
    }

    @Test
    void savesNewRecord() {
        when(repository.save(entity)).thenReturn(entity);
        assertThat(service.addCategorieProduit(entity)).isSameAs(entity);
    }

    @Test
    void retrievesExistingRecord() {
        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        assertThat(service.retrieveCategorieProduit(1L)).isSameAs(entity);
    }

    @Test
    void rejectsMissingRecord() {
        assertThatThrownBy(() -> service.retrieveCategorieProduit(99L))
                .isInstanceOf(CategorieProduitNotFoundException.class);
    }

    @Test
    void updatesExistingRecordUsingItsIdentifier() {
        CategorieProduit changes = new CategorieProduit();
        changes.setIdCategorieProduit(1L);
        changes.setCodeCategorie("CAT-2");
        changes.setLibelleCategorie("Updated label");
        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        when(repository.save(entity)).thenReturn(entity);

        CategorieProduit result = service.updateCategorieProduit(changes);

        assertThat(result).isSameAs(entity);
        assertThat(result.getIdCategorieProduit()).isEqualTo(1L);
        assertThat(result.getCodeCategorie()).isEqualTo("CAT-2");
        assertThat(result.getLibelleCategorie()).isEqualTo("Updated label");
        verify(repository).save(entity);
    }

    @Test
    void missingRecordCannotBeUpdated() {
        assertThatThrownBy(() -> service.updateCategorieProduit(entity))
                .isInstanceOf(CategorieProduitNotFoundException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void deletesExistingRecord() {
        when(repository.existsById(1L)).thenReturn(true);
        service.deleteCategorieProduit(1L);
        verify(repository).deleteById(1L);
    }

    @Test
    void missingRecordCannotBeDeleted() {
        assertThatThrownBy(() -> service.deleteCategorieProduit(99L))
                .isInstanceOf(CategorieProduitNotFoundException.class);
        verify(repository, never()).deleteById(any());
    }
}
