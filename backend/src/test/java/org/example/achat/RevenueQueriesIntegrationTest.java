package org.example.achat;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Date;

import org.example.achat.facture.services.IFactureService;
import org.example.achat.reglements.services.IReglementService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class RevenueQueriesIntegrationTest extends MySqlIntegrationSupport {

    @Autowired private IReglementService paymentService;
    @Autowired private IFactureService invoiceService;

    @Test
    void revenueIncludesBothDateBoundariesAndExcludesArchivedInvoices() {
        // Invoice creation date is outside the range: revenue follows payment dates.
        var active = invoice(200, "2025-12-01", false);
        var archived = invoice(900, "2026-01-10", true);
        payment(active, 10, "2025-12-31");
        payment(active, 12.5f, "2026-01-01");
        payment(active, 25, "2026-01-15");
        payment(active, 37.5f, "2026-01-31");
        payment(active, 20, "2026-02-01");
        payment(archived, 900, "2026-01-15");

        assertThat(paymentService.getRevenueBetweenDates(
                Date.valueOf("2026-01-01"), Date.valueOf("2026-01-31"))).isEqualTo(75);
    }

    @Test
    void sameDayRangeIncludesOnlyThatDay() {
        var invoice = invoice(100, "2026-01-01", false);
        payment(invoice, 10, "2026-01-14");
        payment(invoice, 25, "2026-01-15");
        payment(invoice, 20, "2026-01-16");
        Date date = Date.valueOf("2026-01-15");

        assertThat(paymentService.getRevenueBetweenDates(date, date)).isEqualTo(25);
    }

    @Test
    void emptyRangeReturnsZeroRatherThanNull() {
        Date start = Date.valueOf("2026-01-01");
        Date end = Date.valueOf("2026-01-31");
        assertThat(payments.getRevenueBetweenDates(start, end)).isNull();
        assertThat(paymentService.getRevenueBetweenDates(start, end)).isZero();

        // Nonempty table, but every payment is excluded by the archive filter.
        payment(invoice(100, "2026-01-10", true), 100, "2026-01-10");
        assertThat(paymentService.getRevenueBetweenDates(start, end)).isZero();
    }

    @Test
    void invoiceTotalsUseCreationDatesAndExcludeArchivedInvoices() {
        invoice(500, "2025-12-31", false);
        invoice(100, "2026-01-01", false);
        invoice(200, "2026-01-15", false);
        invoice(300, "2026-01-31", false);
        invoice(500, "2026-02-01", false);
        invoice(900, "2026-01-15", true);

        assertThat(invoices.getTotalFacturesEntreDeuxDates(
                Date.valueOf("2026-01-01"), Date.valueOf("2026-01-31"))).isEqualTo(600);
    }

    @Test
    void recoveryPercentageCombinesRealInvoiceAndPaymentQueries() {
        var invoice = invoice(200, "2026-01-10", false);
        payment(invoice, 50, "2026-01-15");
        payment(invoice, 50, "2026-01-20");
        payment(invoice(900, "2026-01-10", true), 900, "2026-01-15");

        assertThat(invoiceService.pourcentageRecouvrement(
                Date.valueOf("2026-01-01"), Date.valueOf("2026-01-31"))).isEqualTo(50);
    }

    @Test
    void paymentQueryDoesNotMixInvoices() {
        var first = invoice(100, "2026-01-01", false);
        var second = invoice(200, "2026-01-01", false);
        var firstPayment = payment(first, 25, "2026-01-10");
        var secondPayment = payment(first, 50, "2026-01-11");
        payment(second, 200, "2026-01-10");

        assertThat(payments.findByFactureId(first.getIdFacture()))
                .extracting(p -> p.getIdReglement())
                .containsExactlyInAnyOrder(firstPayment.getIdReglement(), secondPayment.getIdReglement());
        assertThat(payments.findByFactureId(Long.MAX_VALUE)).isEmpty();
    }

    @Test
    void lowStockQueryUsesStrictlyBelowMinimum() {
        var low = stock("Low", 2, 5);
        stock("At minimum", 5, 5);
        stock("Enough", 8, 5);

        assertThat(stocks.findStocksBelowMinimumQuantity()).extracting(s -> s.getIdStock())
                .containsExactly(low.getIdStock());
    }
}
