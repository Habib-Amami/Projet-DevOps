package org.example.achat.fournisseurs.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Duration;
import java.util.Date;
import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Level;

import org.example.achat.fournisseurs.entities.CategorieFournisseur;
import org.example.achat.fournisseurs.entities.DetailFournisseur;
import org.example.achat.fournisseurs.entities.Fournisseur;
import org.example.achat.fournisseurs.repositories.FournisseurRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

@ActiveProfiles("test")
@Import(FournisseurServiceImplIntegrationTest.ContainersConfig.class)
@SpringBootTest
class FournisseurServiceImplIntegrationTest {

    private static final Logger log = Logger.getLogger(FournisseurServiceImplIntegrationTest.class.getName());

    @TestConfiguration(proxyBeanMethods = false)
    static class ContainersConfig {
        @Bean
        @ServiceConnection
        @SuppressWarnings("resource")
        MySQLContainer mysqlContainer() {
            return new MySQLContainer(DockerImageName.parse("mysql:8.4"))
                    .withTmpFs(Map.of("/var/lib/mysql", "rw"))
                    .withStartupTimeout(Duration.ofMinutes(5));
        }
    }

    @Autowired
    private IFournisseurService fournisseurService;

    @Autowired
    private FournisseurRepository fournisseurRepository;

    // -----------------------------------------------------------------------
    // Méthode utilitaire : construit un Fournisseur valide avec son detail
    // -----------------------------------------------------------------------

    /**
     * Crée un {@link Fournisseur} prêt à être persisté avec un
     * {@link DetailFournisseur} valide (matricule et adresse @NotBlank remplis).
     */
    private Fournisseur buildFournisseur(String code, String libelle, String matricule) {
        DetailFournisseur detail = new DetailFournisseur();
        detail.setMatricule(matricule);
        detail.setAdresse("123 Rue de Tunis");
        detail.setEmail("contact@fournisseur.tn");
        detail.setDateDebutCollaboration(new Date());

        Fournisseur f = new Fournisseur();
        f.setCode(code);
        f.setLibelle(libelle);
        f.setCategorieFournisseur(CategorieFournisseur.ORDINAIRE);
        f.setDetailFournisseur(detail);
        return f;
    }

    // -----------------------------------------------------------------------
    // Tests
    // -----------------------------------------------------------------------

    @Test
    void addFournisseurPersistsFournisseurWithGeneratedId() {
        // Création
        Fournisseur f = buildFournisseur("F-INT-001", "Fournisseur Intégration", "MAT-001");

        // Ajout
        Fournisseur saved = fournisseurService.addFournisseur(f);
        log.info("Fournisseur ajouté : " + saved);

        // Assertions
        assertNotNull(saved.getIdFournisseur());
        assertEquals("F-INT-001", saved.getCode());
        assertEquals("Fournisseur Intégration", saved.getLibelle());
        assertEquals(CategorieFournisseur.ORDINAIRE, saved.getCategorieFournisseur());
        assertNotNull(saved.getDetailFournisseur());
        assertNotNull(saved.getDetailFournisseur().getDateDebutCollaboration());

        // Nettoyage
        fournisseurService.deleteFournisseur(saved.getIdFournisseur());
    }

    @Test
    void retrieveFournisseurReturnsPreviouslyPersistedFournisseur() {
        // Création
        Fournisseur f = buildFournisseur("F-INT-002", "Fournisseur Retrieve", "MAT-002");
        Fournisseur saved = fournisseurService.addFournisseur(f);

        // Récupération
        Fournisseur found = fournisseurService.retrieveFournisseur(saved.getIdFournisseur());
        log.info("Fournisseur récupéré : " + found);

        // Assertions
        assertNotNull(found);
        assertNotNull(found.getIdFournisseur());
        assertEquals("F-INT-002", found.getCode());
        assertEquals("Fournisseur Retrieve", found.getLibelle());
        assertEquals(CategorieFournisseur.ORDINAIRE, found.getCategorieFournisseur());

        // Nettoyage
        fournisseurService.deleteFournisseur(saved.getIdFournisseur());
    }

    @Test
    void updateFournisseurPersistsModifiedFields() {
        // Création
        Fournisseur f = buildFournisseur("F-INT-003", "Avant Modification", "MAT-003");
        Fournisseur saved = fournisseurService.addFournisseur(f);

        // Modification
        Fournisseur changes = new Fournisseur();
        changes.setIdFournisseur(saved.getIdFournisseur());
        changes.setCode("F-INT-003-UPD");
        changes.setLibelle("Après Modification");
        changes.setCategorieFournisseur(CategorieFournisseur.CONVENTIONNE);

        Fournisseur updated = fournisseurService.updateFournisseur(changes);
        log.info("Fournisseur mis à jour : " + updated);

        // Assertions sur le retour du service
        assertNotNull(updated);
        assertEquals("F-INT-003-UPD", updated.getCode());
        assertEquals("Après Modification", updated.getLibelle());
        assertEquals(CategorieFournisseur.CONVENTIONNE, updated.getCategorieFournisseur());

        // Vérification directe en base
        Fournisseur fromDb = fournisseurService.retrieveFournisseur(saved.getIdFournisseur());
        assertEquals("F-INT-003-UPD", fromDb.getCode());

        // Nettoyage
        fournisseurService.deleteFournisseur(saved.getIdFournisseur());
    }

    @Test
    void deleteFournisseurRemovesFournisseurFromDatabase() {
        // Création
        Fournisseur f = buildFournisseur("F-INT-004", "A Supprimer", "MAT-004");
        Fournisseur saved = fournisseurService.addFournisseur(f);
        Long id = saved.getIdFournisseur();
        log.info("Fournisseur à supprimer (id=" + id + ") : " + saved);

        // Suppression
        fournisseurService.deleteFournisseur(id);

        // Assertions : ne doit plus exister en base
        assertFalse(fournisseurRepository.existsById(id));
    }
}
