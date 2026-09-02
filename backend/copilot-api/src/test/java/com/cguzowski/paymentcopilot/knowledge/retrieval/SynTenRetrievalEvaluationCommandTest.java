package com.cguzowski.paymentcopilot.knowledge.retrieval;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.boot.ApplicationArguments;

class SynTenRetrievalEvaluationCommandTest {

    @Test
    void writesAPassingEvaluationAndReturnsNormally() {
        Fixture fixture = fixture(true);

        fixture.command().run(mock(ApplicationArguments.class));

        verify(fixture.writer()).write(fixture.run());
    }

    @Test
    void preservesAFailingArtifactBeforeExitingNonzeroWithExactFailedVariants() {
        Fixture fixture = fixture(false);

        assertThatThrownBy(() -> fixture.command().run(mock(ApplicationArguments.class)))
                .isInstanceOf(SynTenRetrievalEvaluationFailedException.class)
                .hasMessageContaining("KQ-001/S001")
                .hasMessageContaining("0123456789abcdef0123456789abcdef-FAIL.json");
        InOrder order = inOrder(fixture.service(), fixture.writer());
        order.verify(fixture.service()).evaluate(fixture.seed(), fixture.contract());
        order.verify(fixture.writer()).write(fixture.run());
    }

    private static Fixture fixture(boolean passed) {
        SynTenRetrievalEvaluationSeedRepository seeds = mock(SynTenRetrievalEvaluationSeedRepository.class);
        SynTenRetrievalEvaluationContractRepository contracts = mock(SynTenRetrievalEvaluationContractRepository.class);
        SynTenRetrievalEvaluationService service = mock(SynTenRetrievalEvaluationService.class);
        SynTenRetrievalEvaluationArtifactWriter writer = mock(SynTenRetrievalEvaluationArtifactWriter.class);
        SynTenRetrievalEvaluationSeed seed = mock(SynTenRetrievalEvaluationSeed.class);
        SynTenRetrievalEvaluationContract contract = mock(SynTenRetrievalEvaluationContract.class);
        SynTenRetrievalEvaluationRun run = mock(SynTenRetrievalEvaluationRun.class);
        SynTenRetrievalEvaluationGrade grade = mock(SynTenRetrievalEvaluationGrade.class);
        EvaluationVariantGrade variant = new EvaluationVariantGrade(
                "KQ-001", "S001", true, true, false, true, true, true, true, true, false, List.of("primary missing"));
        Instant evaluatedAt = Instant.parse("2026-09-01T12:00:00Z");
        Path resultPath =
                Path.of("results", "0123456789abcdef0123456789abcdef-" + (passed ? "PASS" : "FAIL") + ".json");
        when(seeds.load()).thenReturn(seed);
        when(seed.evaluatedAt()).thenReturn(evaluatedAt);
        when(contracts.load(evaluatedAt)).thenReturn(contract);
        when(service.evaluate(seed, contract)).thenReturn(run);
        when(run.grade()).thenReturn(grade);
        when(grade.passed()).thenReturn(passed);
        when(grade.variants()).thenReturn(passed ? List.of() : List.of(variant));
        when(writer.write(run)).thenReturn(resultPath);
        return new Fixture(
                new SynTenRetrievalEvaluationCommand(seeds, contracts, service, writer),
                seed,
                contract,
                run,
                service,
                writer);
    }

    private record Fixture(
            SynTenRetrievalEvaluationCommand command,
            SynTenRetrievalEvaluationSeed seed,
            SynTenRetrievalEvaluationContract contract,
            SynTenRetrievalEvaluationRun run,
            SynTenRetrievalEvaluationService service,
            SynTenRetrievalEvaluationArtifactWriter writer) {}
}
