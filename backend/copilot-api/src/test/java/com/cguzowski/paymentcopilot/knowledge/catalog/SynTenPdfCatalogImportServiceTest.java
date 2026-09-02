package com.cguzowski.paymentcopilot.knowledge.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class SynTenPdfCatalogImportServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-31T18:00:00Z");

    @Test
    void validatesTheWholeCorpusPlanBeforeOneAtomicWrite() {
        SynTenPdfCatalogPlanner planner = mock(SynTenPdfCatalogPlanner.class);
        SynTenPdfCatalogPersistenceService persistence = mock(SynTenPdfCatalogPersistenceService.class);
        SynTenPdfCatalogPlan plan = plan();
        when(planner.plan()).thenReturn(plan);
        when(persistence.importAll(plan, NOW)).thenReturn(new PdfCatalogImportSummary(30, 0, 705));
        SynTenPdfCatalogImportService service =
                new SynTenPdfCatalogImportService(planner, persistence, Clock.fixed(NOW, ZoneOffset.UTC));

        PdfCatalogImportSummary summary = service.importCorpus();

        assertThat(summary).isEqualTo(new PdfCatalogImportSummary(30, 0, 705));
        InOrder order = inOrder(planner, persistence);
        order.verify(planner).plan();
        order.verify(persistence).importAll(plan, NOW);
    }

    @Test
    void doesNotWriteWhenCatalogPlanningFails() {
        SynTenPdfCatalogPlanner planner = mock(SynTenPdfCatalogPlanner.class);
        SynTenPdfCatalogPersistenceService persistence = mock(SynTenPdfCatalogPersistenceService.class);
        when(planner.plan()).thenThrow(new IllegalArgumentException("invalid second PDF"));
        SynTenPdfCatalogImportService service =
                new SynTenPdfCatalogImportService(planner, persistence, Clock.fixed(NOW, ZoneOffset.UTC));

        assertThatThrownBy(service::importCorpus)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("invalid second PDF");
        verify(persistence, never()).importAll(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void reusesTheSameDeterministicPlanRepresentation() {
        SynTenPdfCatalogPlanner planner = mock(SynTenPdfCatalogPlanner.class);
        SynTenPdfCatalogPersistenceService persistence = mock(SynTenPdfCatalogPersistenceService.class);
        SynTenPdfCatalogPlan plan = plan();
        when(planner.plan()).thenReturn(plan);
        when(persistence.importAll(plan, NOW)).thenReturn(new PdfCatalogImportSummary(0, 30, 0));
        SynTenPdfCatalogImportService service =
                new SynTenPdfCatalogImportService(planner, persistence, Clock.fixed(NOW, ZoneOffset.UTC));

        service.importCorpus();
        service.importCorpus();

        verify(planner, times(2)).plan();
        verify(persistence, times(2)).importAll(plan, NOW);
    }

    private static SynTenPdfCatalogPlan plan() {
        return new SynTenPdfCatalogPlan(SynTenCorpusSourceRepository.SYNTEN_TENANT_ID, "a".repeat(64), List.of());
    }
}
