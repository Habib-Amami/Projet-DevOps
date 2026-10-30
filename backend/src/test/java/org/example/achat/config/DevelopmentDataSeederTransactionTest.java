package org.example.achat.config;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import org.example.achat.facture.repositories.DetailFactureRepository;
import org.example.achat.facture.repositories.FactureRepository;
import org.example.achat.fournisseurs.repositories.DetailFournisseurRepository;
import org.example.achat.fournisseurs.repositories.FournisseurRepository;
import org.example.achat.fournisseurs.repositories.SecteurActiviteRepository;
import org.example.achat.operateur.repositories.OperateurRepository;
import org.example.achat.produit.repositories.CategorieProduitRepository;
import org.example.achat.produit.repositories.ProduitRepository;
import org.example.achat.reglements.repositories.ReglementRepository;
import org.example.achat.stock.entities.Stock;
import org.example.achat.stock.repositories.StockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedConstruction;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.SimpleTransactionStatus;

class DevelopmentDataSeederTransactionTest {

    private final StockRepository stockRepository = mock(StockRepository.class);
    private final PlatformTransactionManager transactions = mock(PlatformTransactionManager.class);
    private final TransactionStatus transactionStatus = new SimpleTransactionStatus();
    private ApplicationRunner seeder;

    @BeforeEach
    void createTransactionalSeeder() {
        when(stockRepository.findAll()).thenReturn(new ArrayList<>());
        when(stockRepository.save(any(Stock.class))).thenAnswer(returnsFirstArg());
        when(transactions.getTransaction(any())).thenReturn(transactionStatus);

        DevelopmentDataSeeder target = new DevelopmentDataSeeder(
                stockRepository, mock(CategorieProduitRepository.class), mock(ProduitRepository.class),
                mock(SecteurActiviteRepository.class), mock(DetailFournisseurRepository.class),
                mock(FournisseurRepository.class), mock(OperateurRepository.class),
                mock(FactureRepository.class), mock(DetailFactureRepository.class), mock(ReglementRepository.class));
        TransactionInterceptor interceptor = new TransactionInterceptor();
        interceptor.setTransactionManager(transactions);
        interceptor.setTransactionAttributeSource(new AnnotationTransactionAttributeSource());
        ProxyFactory factory = new ProxyFactory(target);
        factory.addAdvice(interceptor);
        seeder = (ApplicationRunner) factory.getProxy();
    }

    @ParameterizedTest(name = "Seed failure rolls back; malformed JSON = {0}")
    @ValueSource(booleans = {false, true})
    void checkedSeedFailureAfterAnInsertRollsBack(boolean malformedJson) {
        try (MockedConstruction<ClassPathResource> resources = mockConstruction(ClassPathResource.class,
                (resource, context) -> {
                    String path = (String) context.arguments().getFirst();
                    if (path.equals("seed/stocks.json")) {
                        when(resource.getInputStream()).thenReturn(input("""
                                [{"label":"Rollback test","quantity":10,"minimumQuantity":2}]
                                """));
                    } else if (malformedJson) {
                        when(resource.getInputStream()).thenReturn(input("{"));
                    } else {
                        when(resource.getInputStream()).thenThrow(new IOException("Seed resource cannot be read"));
                    }
                })) {
            assertThatThrownBy(() -> seeder.run(new DefaultApplicationArguments()))
                    .isInstanceOf(IOException.class);

            verify(stockRepository).save(any(Stock.class));
            verify(transactions).rollback(transactionStatus);
            verify(transactions, never()).commit(any());
        }
    }

    @Test
    void successfulSeedCommits() throws Exception {
        try (MockedConstruction<ClassPathResource> resources = mockConstruction(ClassPathResource.class,
                (resource, context) -> when(resource.getInputStream()).thenReturn(input("[]")))) {
            seeder.run(new DefaultApplicationArguments());

            verify(transactions).commit(transactionStatus);
            verify(transactions, never()).rollback(any());
        }
    }

    private static ByteArrayInputStream input(String json) {
        return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
    }
}
