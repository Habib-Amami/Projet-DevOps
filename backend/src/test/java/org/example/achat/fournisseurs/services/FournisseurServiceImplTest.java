package org.example.achat.fournisseurs.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.example.achat.fournisseurs.entities.CategorieFournisseur;
import org.example.achat.fournisseurs.entities.Fournisseur;
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

    @Mock
    private FournisseurRepository fournisseurRepository;

    @Mock
    private DetailFournisseurRepository detailFournisseurRepository;

    @Mock
    private SecteurActiviteRepository secteurActiviteRepository;

    @InjectMocks
    private FournisseurServiceImpl fournisseurService;

    private Fournisseur fournisseur;

    @BeforeEach
    void setUp() {
        fournisseur = new Fournisseur();
        fournisseur.setIdFournisseur(1L);
        fournisseur.setCode("F-001");
        fournisseur.setLibelle("Fournisseur Test");
        fournisseur.setCategorieFournisseur(CategorieFournisseur.ORDINAIRE);
    }

    @Test
    void addFournisseurSavesAndReturnsFournisseur() {
        when(fournisseurRepository.save(fournisseur)).thenReturn(fournisseur);

        Fournisseur saved = fournisseurService.addFournisseur(fournisseur);

        assertThat(saved).isSameAs(fournisseur);
        assertThat(saved.getCode()).isEqualTo("F-001");
        assertThat(saved.getCategorieFournisseur()).isEqualTo(CategorieFournisseur.ORDINAIRE);
        verify(fournisseurRepository).save(fournisseur);
    }

    @Test
    void retrieveFournisseurReturnsExistingFournisseur() {
        when(fournisseurRepository.findById(1L)).thenReturn(Optional.of(fournisseur));

        Fournisseur found = fournisseurService.retrieveFournisseur(1L);

        assertThat(found).isSameAs(fournisseur);
        assertThat(found.getIdFournisseur()).isEqualTo(1L);
        assertThat(found.getLibelle()).isNotEmpty();
        verify(fournisseurRepository).findById(1L);
    }

    @Test
    void updateFournisseurCopiesFieldsAndSavesExistingFournisseur() {
        Fournisseur changes = new Fournisseur();
        changes.setIdFournisseur(1L);
        changes.setCode("F-002");
        changes.setLibelle("Fournisseur Modifié");
        changes.setCategorieFournisseur(CategorieFournisseur.CONVENTIONNE);

        when(fournisseurRepository.findById(1L)).thenReturn(Optional.of(fournisseur));
        when(fournisseurRepository.save(fournisseur)).thenReturn(fournisseur);

        Fournisseur result = fournisseurService.updateFournisseur(changes);

        assertThat(result).isSameAs(fournisseur);
        assertThat(fournisseur.getCode()).isEqualTo("F-002");
        assertThat(fournisseur.getLibelle()).isEqualTo("Fournisseur Modifié");
        assertThat(fournisseur.getCategorieFournisseur()).isEqualTo(CategorieFournisseur.CONVENTIONNE);
        verify(fournisseurRepository).save(fournisseur);
    }

    @Test
    void deleteFournisseurDeletesExistingFournisseur() {
        when(fournisseurRepository.existsById(1L)).thenReturn(true);

        fournisseurService.deleteFournisseur(1L);

        verify(fournisseurRepository).existsById(1L);
        verify(fournisseurRepository).deleteById(1L);
    }
}
