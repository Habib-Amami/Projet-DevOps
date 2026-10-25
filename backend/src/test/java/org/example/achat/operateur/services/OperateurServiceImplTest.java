package org.example.achat.operateur.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.example.achat.facture.entities.Facture;

import org.example.achat.operateur.entities.Operateur;
import org.example.achat.operateur.exceptions.OperateurNotFoundException;
import org.example.achat.operateur.repositories.OperateurRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OperateurServiceImplTest {

    @Mock
    private OperateurRepository operateurRepository;

    @InjectMocks
    private OperateurServiceImpl operateurService;

    private Operateur operateur;

    @BeforeEach
    void setUp() {
        operateur = new Operateur();
        operateur.setIdOperateur(1L);
        operateur.setNom("Ben Ali");
        operateur.setPrenom("Sami");
        operateur.setPassword("secret");
    }

    @Test
    void retrieveAllOperateursReturnsRepositoryResults() {
        when(operateurRepository.findAll()).thenReturn(List.of(operateur));

        assertThat(operateurService.retrieveAllOperateurs()).containsExactly(operateur);

        verify(operateurRepository).findAll();
    }

    @Test
    void addOperateurSavesAndReturnsOperateur() {
        when(operateurRepository.save(operateur)).thenReturn(operateur);

        assertThat(operateurService.addOperateur(operateur)).isSameAs(operateur);

        verify(operateurRepository).save(operateur);
    }

    @Test
    void retrieveOperateurReturnsExistingOperateur() {
        when(operateurRepository.findById(1L)).thenReturn(Optional.of(operateur));

        assertThat(operateurService.retrieveOperateur(1L)).isSameAs(operateur);
    }

    @Test
    void retrieveOperateurThrowsWhenOperateurDoesNotExist() {
        when(operateurRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> operateurService.retrieveOperateur(99L))
                .isInstanceOf(OperateurNotFoundException.class)
                .hasMessage("Operateur not found with id: 99");
    }

    @Test
    void updateOperateurCopiesEditableFieldsAndSavesExistingOperateur() {
        Operateur changes = new Operateur();
        changes.setIdOperateur(1L);
        changes.setNom("Trabelsi");
        changes.setPrenom("Ines");
        changes.setPassword("new-secret");
        when(operateurRepository.findById(1L)).thenReturn(Optional.of(operateur));
        when(operateurRepository.save(operateur)).thenReturn(operateur);

        Operateur result = operateurService.updateOperateur(changes);

        assertThat(result).isSameAs(operateur);
        assertThat(operateur.getNom()).isEqualTo("Trabelsi");
        assertThat(operateur.getPrenom()).isEqualTo("Ines");
        assertThat(operateur.getPassword()).isEqualTo("new-secret");
        verify(operateurRepository).save(operateur);
    }

    @Test
    void updateOperateurKeepsIdAndExistingFactures() {
        Facture facture = new Facture();
        facture.setIdFacture(10L);
        operateur.setFactures(Set.of(facture));
        Operateur changes = new Operateur();
        changes.setIdOperateur(1L);
        changes.setNom("Trabelsi");
        changes.setPrenom("Ines");
        changes.setPassword("new-secret");
        when(operateurRepository.findById(1L)).thenReturn(Optional.of(operateur));
        when(operateurRepository.save(operateur)).thenReturn(operateur);

        operateurService.updateOperateur(changes);

        assertThat(operateur.getIdOperateur()).isEqualTo(1L);
        assertThat(operateur.getFactures()).containsExactly(facture);
    }

    @Test
    void updateOperateurThrowsWhenOperateurDoesNotExist() {
        Operateur changes = new Operateur();
        changes.setIdOperateur(99L);
        when(operateurRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> operateurService.updateOperateur(changes))
                .isInstanceOf(OperateurNotFoundException.class)
                .hasMessage("Operateur not found with id: 99");

        verify(operateurRepository, never()).save(any(Operateur.class));
    }

    @Test
    void deleteOperateurDeletesExistingOperateur() {
        when(operateurRepository.existsById(1L)).thenReturn(true);

        operateurService.deleteOperateur(1L);

        verify(operateurRepository).deleteById(1L);
    }

    @Test
    void deleteOperateurThrowsWhenOperateurDoesNotExist() {
        when(operateurRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> operateurService.deleteOperateur(99L))
                .isInstanceOf(OperateurNotFoundException.class)
                .hasMessage("Operateur not found with id: 99");

        verify(operateurRepository, never()).deleteById(anyLong());
    }
}
