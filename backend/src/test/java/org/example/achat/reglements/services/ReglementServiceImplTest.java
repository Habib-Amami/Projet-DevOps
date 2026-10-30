package org.example.achat.reglements.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.sql.Date;
import java.util.List;
import java.util.Optional;

import org.example.achat.facture.entities.Facture;
import org.example.achat.reglements.entities.Reglement;
import org.example.achat.reglements.exceptions.ReglementNotFoundException;
import org.example.achat.reglements.repositories.ReglementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReglementServiceImplTest {

    @Mock private ReglementRepository repository;
    @InjectMocks private ReglementServiceImpl service;
    private Reglement payment;

    @BeforeEach
    void createPayment() {
        payment = new Reglement();
        payment.setIdReglement(1L);
        payment.setMontantPaye(25);
        payment.setMontantRestant(75);
        payment.setPayee(false);
        payment.setDateReglement(Date.valueOf("2026-01-10"));
        payment.setFacture(new Facture());
    }

    @Test
    void listsPayments() {
        when(repository.findAll()).thenReturn(List.of(payment));
        assertThat(service.retrieveAllReglements()).containsExactly(payment);
    }

    @Test
    void savesPaymentWithItsInvoice() {
        when(repository.save(payment)).thenReturn(payment);
        assertThat(service.addReglement(payment)).isSameAs(payment);
        assertThat(payment.getFacture()).isNotNull();
    }

    @Test
    void retrievesExistingPayment() {
        when(repository.findById(1L)).thenReturn(Optional.of(payment));
        assertThat(service.retrieveReglement(1L)).isSameAs(payment);
    }

    @Test
    void rejectsMissingPayment() {
        assertThatThrownBy(() -> service.retrieveReglement(99L))
                .isInstanceOf(ReglementNotFoundException.class);
    }

    @Test
    void updateChangesAmountsAndDateButPreservesInvoice() {
        Facture originalInvoice = payment.getFacture();
        Reglement changes = new Reglement();
        changes.setIdReglement(1L);
        changes.setMontantPaye(100);
        changes.setMontantRestant(0);
        changes.setPayee(true);
        changes.setDateReglement(Date.valueOf("2026-01-20"));
        changes.setFacture(new Facture());
        when(repository.findById(1L)).thenReturn(Optional.of(payment));
        when(repository.save(payment)).thenReturn(payment);

        Reglement result = service.updateReglement(changes);

        assertThat(result).isSameAs(payment);
        assertThat(result.getMontantPaye()).isEqualTo(100);
        assertThat(result.getMontantRestant()).isZero();
        assertThat(result.getPayee()).isTrue();
        assertThat(result.getDateReglement()).isEqualTo(changes.getDateReglement());
        assertThat(result.getFacture()).isSameAs(originalInvoice);
    }

    @Test
    void updateWithoutInvoiceAlsoPreservesRelationship() {
        Facture originalInvoice = payment.getFacture();
        Reglement changes = new Reglement();
        changes.setIdReglement(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(payment));
        when(repository.save(payment)).thenReturn(payment);

        assertThat(service.updateReglement(changes).getFacture()).isSameAs(originalInvoice);
    }

    @Test
    void missingPaymentCannotBeUpdated() {
        payment.setIdReglement(99L);
        assertThatThrownBy(() -> service.updateReglement(payment))
                .isInstanceOf(ReglementNotFoundException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void deletesExistingPayment() {
        when(repository.existsById(1L)).thenReturn(true);
        service.deleteReglement(1L);
        verify(repository).deleteById(1L);
    }

    @Test
    void missingPaymentCannotBeDeleted() {
        assertThatThrownBy(() -> service.deleteReglement(99L))
                .isInstanceOf(ReglementNotFoundException.class);
        verify(repository, never()).deleteById(any());
    }

    @Test
    void filtersPaymentsByInvoice() {
        when(repository.findByFactureId(10L)).thenReturn(List.of(payment));
        assertThat(service.findByFactureId(10L)).containsExactly(payment);
    }

    @ParameterizedTest
    @ValueSource(floats = {0, 42.5f})
    void returnsRevenueIncludingAnEqualDateRange(float amount) {
        Date date = Date.valueOf("2026-01-10");
        when(repository.getRevenueBetweenDates(date, date)).thenReturn(amount);
        assertThat(service.getRevenueBetweenDates(date, date)).isEqualTo(amount);
    }

    @Test
    void emptyRevenueIsZero() {
        Date start = Date.valueOf("2026-01-01");
        Date end = Date.valueOf("2026-01-31");
        when(repository.getRevenueBetweenDates(start, end)).thenReturn(null);
        assertThat(service.getRevenueBetweenDates(start, end)).isZero();
    }

    @Test
    void rejectsReversedDatesBeforeQueryingDatabase() {
        assertThatThrownBy(() -> service.getRevenueBetweenDates(
                Date.valueOf("2026-02-01"), Date.valueOf("2026-01-01")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Start date must be before or equal to end date");
        verifyNoInteractions(repository);
    }
}
