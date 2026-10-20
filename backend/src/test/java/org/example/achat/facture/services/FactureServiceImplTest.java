package org.example.achat.facture.services;

import org.example.achat.facture.entities.Facture;
import org.example.achat.facture.repositories.FactureRepository;
import org.example.achat.fournisseurs.exceptions.FournisseurNotFoundException;
import org.example.achat.fournisseurs.repositories.FournisseurRepository;
import org.example.achat.operateur.entities.Operateur;
import org.example.achat.operateur.exceptions.OperateurNotFoundException;
import org.example.achat.operateur.repositories.OperateurRepository;
import org.example.achat.reglements.services.ReglementServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FactureServiceImplTest {

    @Mock
    FactureRepository factureRepository;
    @Mock
    OperateurRepository operateurRepository;
    @Mock
    FournisseurRepository fournisseurRepository;
    @Mock
    ReglementServiceImpl reglementService;

    @InjectMocks
    FactureServiceImpl factureService;

    // ---------- retrieveAllFactures ----------

    @Test
    @DisplayName("retrieveAllFactures retourne toutes les factures")
    void retrieveAllFactures_returnsAll() {
        List<Facture> factures = List.of(new Facture(), new Facture());
        when(factureRepository.findAll()).thenReturn(factures);

        List<Facture> result = factureService.retrieveAllFactures();

        assertEquals(2, result.size());
        verify(factureRepository).findAll();
    }

    @Test
    @DisplayName("retrieveAllFactures retourne une liste vide si aucune facture")
    void retrieveAllFactures_empty() {
        when(factureRepository.findAll()).thenReturn(List.of());

        assertTrue(factureService.retrieveAllFactures().isEmpty());
    }

    // ---------- addFacture ----------

    @Test
    @DisplayName("addFacture sauvegarde et retourne la facture")
    void addFacture_saves() {
        Facture f = new Facture();
        f.setMontantFacture(100f);
        when(factureRepository.save(f)).thenReturn(f);

        Facture result = factureService.addFacture(f);

        assertSame(f, result);
        verify(factureRepository).save(f);
    }

    // ---------- retrieveFacture ----------

    @Test
    @DisplayName("retrieveFacture retourne la facture si elle existe")
    void retrieveFacture_found() {
        Facture f = new Facture();
        f.setIdFacture(1L);
        when(factureRepository.findById(1L)).thenReturn(Optional.of(f));

        assertSame(f, factureService.retrieveFacture(1L));
    }

    @Test
    @DisplayName("retrieveFacture retourne null si la facture n'existe pas")
    void retrieveFacture_notFound() {
        when(factureRepository.findById(99L)).thenReturn(Optional.empty());

        assertNull(factureService.retrieveFacture(99L));
    }

    // ---------- cancelFacture ----------

    @Test
    @DisplayName("cancelFacture archive la facture existante")
    void cancelFacture_existing() {
        Facture f = new Facture();
        f.setIdFacture(1L);
        f.setArchivee(false);
        when(factureRepository.findById(1L)).thenReturn(Optional.of(f));

        factureService.cancelFacture(1L);

        assertTrue(f.getArchivee());
        verify(factureRepository).save(f);
        verify(factureRepository).updateFacture(1L);
    }

    /**
     * Documente le comportement ACTUEL : si l'id n'existe pas, le service
     * crée et sauvegarde une nouvelle facture vide archivée (orElse(new Facture())).
     * À corriger dans le service (lever une exception) puis adapter ce test.
     */
    @Test
    @DisplayName("cancelFacture sur id inconnu sauvegarde une facture vide archivée (comportement actuel)")
    void cancelFacture_notFound_savesNewArchivedFacture() {
        when(factureRepository.findById(99L)).thenReturn(Optional.empty());

        factureService.cancelFacture(99L);

        verify(factureRepository).save(argThat(f -> Boolean.TRUE.equals(f.getArchivee())));
        verify(factureRepository).updateFacture(99L);
    }

    // ---------- getFacturesByFournisseur ----------

    @Test
    @DisplayName("getFacturesByFournisseur retourne les factures du fournisseur")
    void getFacturesByFournisseur_ok() {
        List<Facture> factures = List.of(new Facture());
        when(fournisseurRepository.existsById(1L)).thenReturn(true);
        when(factureRepository.findByFournisseurIdFournisseur(1L)).thenReturn(factures);

        List<Facture> result = factureService.getFacturesByFournisseur(1L);

        assertEquals(1, result.size());
        verify(factureRepository).findByFournisseurIdFournisseur(1L);
    }

    @Test
    @DisplayName("getFacturesByFournisseur lève FournisseurNotFoundException si fournisseur inconnu")
    void getFacturesByFournisseur_notFound() {
        when(fournisseurRepository.existsById(99L)).thenReturn(false);

        assertThrows(FournisseurNotFoundException.class,
                () -> factureService.getFacturesByFournisseur(99L));

        verify(factureRepository, never()).findByFournisseurIdFournisseur(anyLong());
    }

    // ---------- assignOperateurToFacture ----------

    @Test
    @DisplayName("assignOperateurToFacture ajoute la facture à l'opérateur et le sauvegarde")
    void assignOperateurToFacture_ok() {
        Facture facture = new Facture();
        facture.setIdFacture(1L);
        Set<Facture> factures = new HashSet<>();
        Operateur operateur = mock(Operateur.class);
        when(operateur.getFactures()).thenReturn(factures); // adapter si getFactures() renvoie une List

        when(factureRepository.findById(1L)).thenReturn(Optional.of(facture));
        when(operateurRepository.findById(2L)).thenReturn(Optional.of(operateur));

        factureService.assignOperateurToFacture(2L, 1L);

        assertTrue(factures.contains(facture));
        verify(operateurRepository).save(operateur);
    }

    @Test
    @DisplayName("assignOperateurToFacture lève OperateurNotFoundException si opérateur inconnu")
    void assignOperateurToFacture_operateurNotFound() {
        when(factureRepository.findById(1L)).thenReturn(Optional.of(new Facture()));
        when(operateurRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(OperateurNotFoundException.class,
                () -> factureService.assignOperateurToFacture(99L, 1L));

        verify(operateurRepository, never()).save(any());
    }

    // ---------- pourcentageRecouvrement ----------

    @Test
    @DisplayName("pourcentageRecouvrement calcule le bon pourcentage")
    void pourcentageRecouvrement_ok() {
        Date start = new Date(0L);
        Date end = new Date();
        when(factureRepository.getTotalFacturesEntreDeuxDates(start, end)).thenReturn(1000f);
        when(reglementService.getRevenueBetweenDates(start, end)).thenReturn(500f);

        float pourcentage = factureService.pourcentageRecouvrement(start, end);

        assertEquals(50f, pourcentage, 0.001f);
    }

    @Test
    @DisplayName("pourcentageRecouvrement retourne 100 si tout est réglé")
    void pourcentageRecouvrement_full() {
        Date start = new Date(0L);
        Date end = new Date();
        when(factureRepository.getTotalFacturesEntreDeuxDates(start, end)).thenReturn(200f);
        when(reglementService.getRevenueBetweenDates(start, end)).thenReturn(200f);

        assertEquals(100f, factureService.pourcentageRecouvrement(start, end), 0.001f);
    }

    /**
     * Documente le comportement ACTUEL : total factures = 0 avec des règlements > 0
     * donne Infinity (division float, pas d'exception). À protéger dans le service.
     */
    @Test
    @DisplayName("pourcentageRecouvrement avec total factures à 0 donne Infinity (comportement actuel)")
    void pourcentageRecouvrement_zeroTotal() {
        Date start = new Date(0L);
        Date end = new Date();
        when(factureRepository.getTotalFacturesEntreDeuxDates(start, end)).thenReturn(0f);
        when(reglementService.getRevenueBetweenDates(start, end)).thenReturn(100f);

        assertTrue(Float.isInfinite(factureService.pourcentageRecouvrement(start, end)));
    }
}
