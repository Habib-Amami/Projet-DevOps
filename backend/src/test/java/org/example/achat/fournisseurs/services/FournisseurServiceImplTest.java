package org.example.achat.fournisseurs.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import org.example.achat.facture.entities.Facture;
import org.example.achat.fournisseurs.entities.CategorieFournisseur;
import org.example.achat.fournisseurs.entities.DetailFournisseur;
import org.example.achat.fournisseurs.entities.Fournisseur;
import org.example.achat.fournisseurs.entities.SecteurActivite;
import org.example.achat.fournisseurs.exceptions.FournisseurNotFoundException;
import org.example.achat.fournisseurs.exceptions.SecteurActiviteNotFoundException;
import org.example.achat.fournisseurs.repositories.DetailFournisseurRepository;
import org.example.achat.fournisseurs.repositories.FournisseurRepository;
import org.example.achat.fournisseurs.repositories.SecteurActiviteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FournisseurServiceImplTest {

    @Mock private FournisseurRepository repository;
    @Mock private DetailFournisseurRepository details;
    @Mock private SecteurActiviteRepository sectors;
    @InjectMocks private FournisseurServiceImpl service;
    private Fournisseur supplier;

    @BeforeEach
    void createSupplier() {
        supplier = new Fournisseur();
        supplier.setIdFournisseur(1L);
        supplier.setCode("SUP-1");
        supplier.setLibelle("Supplier");
        supplier.setCategorieFournisseur(CategorieFournisseur.ORDINAIRE);
        supplier.setDetailFournisseur(new DetailFournisseur());
    }

    @Test
    void listsSuppliers() {
        when(repository.findAll()).thenReturn(List.of(supplier));
        assertThat(service.retrieveAllFournisseurs()).containsExactly(supplier);
    }

    @Test
    void creationPreservesExplicitDetails() {
        DetailFournisseur original = supplier.getDetailFournisseur();
        when(repository.save(supplier)).thenReturn(supplier);
        assertThat(service.addFournisseur(supplier).getDetailFournisseur()).isSameAs(original);
        verifyNoInteractions(details);
    }

    @Test
    void creationWithoutDetailsInitializesCollaborationDate() {
        supplier.setDetailFournisseur(null);
        when(repository.save(supplier)).thenReturn(supplier);
        Fournisseur result = service.addFournisseur(supplier);
        assertThat(result.getDetailFournisseur()).isNotNull();
        assertThat(result.getDetailFournisseur().getDateDebutCollaboration()).isNotNull();
    }

    @Test
    void retrievesExistingSupplier() {
        when(repository.findById(1L)).thenReturn(Optional.of(supplier));
        assertThat(service.retrieveFournisseur(1L)).isSameAs(supplier);
    }

    @Test
    void rejectsMissingSupplier() {
        assertThatThrownBy(() -> service.retrieveFournisseur(99L))
                .isInstanceOf(FournisseurNotFoundException.class);
    }

    @Test
    void updateWithoutDetailsPreservesDetailsInvoicesAndSectors() {
        DetailFournisseur originalDetails = supplier.getDetailFournisseur();
        supplier.setFactures(new HashSet<>(List.of(new Facture())));
        supplier.setSecteurActivites(new HashSet<>(List.of(new SecteurActivite())));
        var originalInvoices = supplier.getFactures();
        var originalSectors = supplier.getSecteurActivites();
        Fournisseur changes = new Fournisseur();
        changes.setIdFournisseur(1L);
        changes.setCode("SUP-2");
        changes.setLibelle("Updated supplier");
        changes.setCategorieFournisseur(CategorieFournisseur.CONVENTIONNE);
        when(repository.findById(1L)).thenReturn(Optional.of(supplier));
        when(repository.save(supplier)).thenReturn(supplier);

        Fournisseur result = service.updateFournisseur(changes);

        assertThat(result.getCode()).isEqualTo("SUP-2");
        assertThat(result.getLibelle()).isEqualTo("Updated supplier");
        assertThat(result.getCategorieFournisseur()).isEqualTo(CategorieFournisseur.CONVENTIONNE);
        assertThat(result.getDetailFournisseur()).isSameAs(originalDetails);
        assertThat(result.getFactures()).isSameAs(originalInvoices);
        assertThat(result.getSecteurActivites()).isSameAs(originalSectors);
    }

    @Test
    void updateAcceptsExplicitReplacementDetails() {
        Fournisseur changes = new Fournisseur();
        changes.setIdFournisseur(1L);
        changes.setDetailFournisseur(new DetailFournisseur());
        when(repository.findById(1L)).thenReturn(Optional.of(supplier));
        when(repository.save(supplier)).thenReturn(supplier);
        assertThat(service.updateFournisseur(changes).getDetailFournisseur())
                .isSameAs(changes.getDetailFournisseur());
    }

    @Test
    void missingSupplierCannotBeUpdated() {
        assertThatThrownBy(() -> service.updateFournisseur(supplier))
                .isInstanceOf(FournisseurNotFoundException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void deletesExistingSupplier() {
        when(repository.existsById(1L)).thenReturn(true);
        service.deleteFournisseur(1L);
        verify(repository).deleteById(1L);
    }

    @Test
    void missingSupplierCannotBeDeleted() {
        assertThatThrownBy(() -> service.deleteFournisseur(99L))
                .isInstanceOf(FournisseurNotFoundException.class);
        verify(repository, never()).deleteById(any());
    }

    @Test
    void assignmentInitializesSectorsAndDoesNotDuplicateExistingAssignment() {
        SecteurActivite sector = new SecteurActivite();
        when(repository.findById(1L)).thenReturn(Optional.of(supplier));
        when(sectors.findById(2L)).thenReturn(Optional.of(sector));

        service.assignSecteurActiviteToFournisseur(2L, 1L);
        service.assignSecteurActiviteToFournisseur(2L, 1L);

        assertThat(supplier.getSecteurActivites()).containsExactly(sector);
    }

    @Test
    void assignmentPreservesOtherSectors() {
        SecteurActivite existing = new SecteurActivite();
        SecteurActivite added = new SecteurActivite();
        supplier.setSecteurActivites(new HashSet<>(List.of(existing)));
        when(repository.findById(1L)).thenReturn(Optional.of(supplier));
        when(sectors.findById(2L)).thenReturn(Optional.of(added));

        service.assignSecteurActiviteToFournisseur(2L, 1L);

        assertThat(supplier.getSecteurActivites()).containsExactlyInAnyOrder(existing, added);
        verify(repository).save(supplier);
    }

    @Test
    void missingSupplierStopsSectorAssignment() {
        assertThatThrownBy(() -> service.assignSecteurActiviteToFournisseur(2L, 99L))
                .isInstanceOf(FournisseurNotFoundException.class);
        verifyNoInteractions(sectors);
        verify(repository, never()).save(any());
    }

    @Test
    void missingSectorDoesNotChangeSupplier() {
        when(repository.findById(1L)).thenReturn(Optional.of(supplier));
        assertThatThrownBy(() -> service.assignSecteurActiviteToFournisseur(99L, 1L))
                .isInstanceOf(SecteurActiviteNotFoundException.class);
        assertThat(supplier.getSecteurActivites()).isNull();
        verify(repository, never()).save(any());
    }
}
