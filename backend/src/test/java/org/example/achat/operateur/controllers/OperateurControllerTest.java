package org.example.achat.operateur.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Set;

import org.example.achat.facture.entities.Facture;
import org.example.achat.operateur.entities.Operateur;
import org.example.achat.operateur.exceptions.OperateurNotFoundException;
import org.example.achat.operateur.services.IOperateurService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OperateurController.class)
@AutoConfigureMockMvc(addFilters = false)
class OperateurControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IOperateurService operateurService;

    @Test
    void getOperateursReturnsOperateurResponses() throws Exception {
        when(operateurService.retrieveAllOperateurs()).thenReturn(List.of(operateur(1L, "Ben Ali", "Sami")));

        mockMvc.perform(get("/operateurs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idOperateur").value(1))
                .andExpect(jsonPath("$[0].nom").value("Ben Ali"))
                .andExpect(jsonPath("$[0].prenom").value("Sami"))
                .andExpect(jsonPath("$[0].factureIds").isEmpty());
    }

    @Test
    void getOperateursReturnsEmptyListWhenNoOperateurExists() throws Exception {
        when(operateurService.retrieveAllOperateurs()).thenReturn(List.of());

        mockMvc.perform(get("/operateurs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void retrieveOperateurReturnsOperateurResponseWithFactureIds() throws Exception {
        Operateur operateur = operateur(1L, "Ben Ali", "Sami");
        operateur.setFactures(Set.of(facture(10L), facture(11L)));
        when(operateurService.retrieveOperateur(1L)).thenReturn(operateur);

        mockMvc.perform(get("/operateurs/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idOperateur").value(1))
                .andExpect(jsonPath("$.nom").value("Ben Ali"))
                .andExpect(jsonPath("$.factureIds", containsInAnyOrder(10, 11)));

        verify(operateurService).retrieveOperateur(1L);
    }

    @Test
    void retrieveOperateurDoesNotExposePassword() throws Exception {
        when(operateurService.retrieveOperateur(1L)).thenReturn(operateur(1L, "Ben Ali", "Sami"));

        mockMvc.perform(get("/operateurs/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void retrieveOperateurRejectsNonNumericId() throws Exception {
        mockMvc.perform(get("/operateurs/abc"))
                .andExpect(status().isBadRequest());

        verify(operateurService, never()).retrieveOperateur(anyLong());
    }

    @Test
    void retrieveMissingOperateurReturnsNotFoundProblemDetail() throws Exception {
        when(operateurService.retrieveOperateur(99L)).thenThrow(new OperateurNotFoundException(99L));

        mockMvc.perform(get("/operateurs/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Operateur not found with id: 99"));
    }

    @Test
    void addOperateurReturnsCreatedResponse() throws Exception {
        when(operateurService.addOperateur(any(Operateur.class))).thenReturn(operateur(1L, "Ben Ali", "Sami"));

        mockMvc.perform(post("/operateurs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(operateurJson("Ben Ali", "Sami", "secret")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idOperateur").value(1))
                .andExpect(jsonPath("$.nom").value("Ben Ali"))
                .andExpect(jsonPath("$.password").doesNotExist());

        ArgumentCaptor<Operateur> operateurCaptor = ArgumentCaptor.forClass(Operateur.class);
        verify(operateurService).addOperateur(operateurCaptor.capture());
        assertThat(operateurCaptor.getValue().getNom()).isEqualTo("Ben Ali");
        assertThat(operateurCaptor.getValue().getPrenom()).isEqualTo("Sami");
        assertThat(operateurCaptor.getValue().getPassword()).isEqualTo("secret");
    }

    @Test
    void addOperateurRejectsInvalidRequest() throws Exception {
        mockMvc.perform(post("/operateurs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(operateurJson("", "", "")))
                .andExpect(status().isBadRequest());

        verify(operateurService, never()).addOperateur(any(Operateur.class));
    }

    @Test
    void addOperateurRejectsMalformedJson() throws Exception {
        mockMvc.perform(post("/operateurs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nom\": "))
                .andExpect(status().isBadRequest());

        verify(operateurService, never()).addOperateur(any(Operateur.class));
    }

    @Test
    void modifyOperateurUsesPathIdAndReturnsUpdatedResponse() throws Exception {
        when(operateurService.updateOperateur(any(Operateur.class))).thenReturn(operateur(1L, "Trabelsi", "Ines"));

        mockMvc.perform(put("/operateurs/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(operateurJson("Trabelsi", "Ines", "new-secret")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idOperateur").value(1))
                .andExpect(jsonPath("$.nom").value("Trabelsi"));

        ArgumentCaptor<Operateur> operateurCaptor = ArgumentCaptor.forClass(Operateur.class);
        verify(operateurService).updateOperateur(operateurCaptor.capture());
        assertThat(operateurCaptor.getValue().getIdOperateur()).isEqualTo(1L);
        assertThat(operateurCaptor.getValue().getNom()).isEqualTo("Trabelsi");
        assertThat(operateurCaptor.getValue().getPrenom()).isEqualTo("Ines");
        assertThat(operateurCaptor.getValue().getPassword()).isEqualTo("new-secret");
    }

    @Test
    void modifyOperateurRejectsInvalidRequest() throws Exception {
        mockMvc.perform(put("/operateurs/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(operateurJson("", "", "")))
                .andExpect(status().isBadRequest());

        verify(operateurService, never()).updateOperateur(any(Operateur.class));
    }

    @Test
    void modifyMissingOperateurReturnsNotFoundProblemDetail() throws Exception {
        when(operateurService.updateOperateur(any(Operateur.class))).thenThrow(new OperateurNotFoundException(99L));

        mockMvc.perform(put("/operateurs/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(operateurJson("Trabelsi", "Ines", "new-secret")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Operateur not found with id: 99"));
    }

    @Test
    void removeOperateurReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/operateurs/1"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(operateurService).deleteOperateur(1L);
    }

    @Test
    void removeMissingOperateurReturnsNotFoundProblemDetail() throws Exception {
        doThrow(new OperateurNotFoundException(99L)).when(operateurService).deleteOperateur(99L);

        mockMvc.perform(delete("/operateurs/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Operateur not found with id: 99"));
    }

    private static Operateur operateur(Long id, String nom, String prenom) {
        Operateur operateur = new Operateur();
        operateur.setIdOperateur(id);
        operateur.setNom(nom);
        operateur.setPrenom(prenom);
        operateur.setPassword("secret");
        return operateur;
    }

    private static Facture facture(Long id) {
        Facture facture = new Facture();
        facture.setIdFacture(id);
        return facture;
    }

    private static String operateurJson(String nom, String prenom, String password) {
        return """
                {
                  "nom": "%s",
                  "prenom": "%s",
                  "password": "%s"
                }
                """.formatted(nom, prenom, password);
    }
}
