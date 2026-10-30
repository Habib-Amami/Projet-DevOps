package org.example.achat.fournisseurs.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.example.achat.fournisseurs.entities.SecteurActivite;
import org.example.achat.fournisseurs.exceptions.SecteurActiviteNotFoundException;
import org.example.achat.fournisseurs.repositories.SecteurActiviteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SecteurActiviteServiceImplTest {

    @Mock private SecteurActiviteRepository repository;
    @InjectMocks private SecteurActiviteServiceImpl service;
    private SecteurActivite entity;

    @BeforeEach
    void createEntity() {
        entity = new SecteurActivite();
        entity.setIdSecteurActivite(1L);
        entity.setCodeSecteurActivite("SECTOR-1");
        entity.setLibelleSecteurActivite("Original label");
    }

    @Test
    void listsExistingRecords() {
        when(repository.findAll()).thenReturn(List.of(entity));
        assertThat(service.retrieveAllSecteurActivite()).containsExactly(entity);
    }

    @Test
    void savesNewRecord() {
        when(repository.save(entity)).thenReturn(entity);
        assertThat(service.addSecteurActivite(entity)).isSameAs(entity);
    }

    @Test
    void retrievesExistingRecord() {
        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        assertThat(service.retrieveSecteurActivite(1L)).isSameAs(entity);
    }

    @Test
    void rejectsMissingRecord() {
        assertThatThrownBy(() -> service.retrieveSecteurActivite(99L))
                .isInstanceOf(SecteurActiviteNotFoundException.class);
    }

    @Test
    void updatesExistingRecordUsingItsIdentifier() {
        SecteurActivite changes = new SecteurActivite();
        changes.setIdSecteurActivite(1L);
        changes.setCodeSecteurActivite("SECTOR-2");
        changes.setLibelleSecteurActivite("Updated label");
        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        when(repository.save(entity)).thenReturn(entity);

        SecteurActivite result = service.updateSecteurActivite(changes);

        assertThat(result).isSameAs(entity);
        assertThat(result.getIdSecteurActivite()).isEqualTo(1L);
        assertThat(result.getCodeSecteurActivite()).isEqualTo("SECTOR-2");
        assertThat(result.getLibelleSecteurActivite()).isEqualTo("Updated label");
        verify(repository).save(entity);
    }

    @Test
    void missingRecordCannotBeUpdated() {
        assertThatThrownBy(() -> service.updateSecteurActivite(entity))
                .isInstanceOf(SecteurActiviteNotFoundException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void deletesExistingRecord() {
        when(repository.existsById(1L)).thenReturn(true);
        service.deleteSecteurActivite(1L);
        verify(repository).deleteById(1L);
    }

    @Test
    void missingRecordCannotBeDeleted() {
        assertThatThrownBy(() -> service.deleteSecteurActivite(99L))
                .isInstanceOf(SecteurActiviteNotFoundException.class);
        verify(repository, never()).deleteById(any());
    }
}
