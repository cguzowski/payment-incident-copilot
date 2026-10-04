package com.cguzowski.syntheticincidentgenerator.comparison;

import com.cguzowski.syntheticincidentgenerator.config.GeneratorProperties;
import com.cguzowski.syntheticincidentgenerator.generation.AnswerKeyRevealResponse;
import com.cguzowski.syntheticincidentgenerator.generation.AnswerKeyRevealService;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

@Service
final class ComparisonService {
    static final String PROMPT_VERSION = "comparison-text-prompt/v1";
    private final AnswerKeyRevealService reveal;
    private final FrozenOutcomeClient outcomes;
    private final TextJudge judge;
    private final ComparisonArtifactStore store;
    private final JsonMapper mapper;
    private final ComparisonProperties properties;
    private final UUID tenantId;
    private final Clock clock;

    @Autowired
    ComparisonService(
            AnswerKeyRevealService reveal,
            FrozenOutcomeClient outcomes,
            TextJudge judge,
            ComparisonArtifactStore store,
            JsonMapper mapper,
            ComparisonProperties properties,
            GeneratorProperties generator,
            Clock clock) {
        this(reveal, outcomes, judge, store, mapper, properties, generator.tenantId(), clock);
    }

    ComparisonService(
            AnswerKeyRevealService reveal,
            FrozenOutcomeClient outcomes,
            TextJudge judge,
            ComparisonArtifactStore store,
            JsonMapper mapper,
            ComparisonProperties properties,
            UUID tenantId,
            Clock clock) {
        this.reveal = reveal;
        this.outcomes = outcomes;
        this.judge = judge;
        this.store = store;
        this.mapper = mapper;
        this.properties = properties;
        this.tenantId = tenantId;
        this.clock = clock;
    }

    ComparisonResponse compare(UUID incidentId, UUID operatorId) {
        Instant requestedAt = clock.instant();
        AnswerKeyRevealResponse key = reveal.reveal(incidentId, operatorId);
        UUID id = UUID.randomUUID();
        FrozenOutcome frozen = null;
        ComparisonGrade grade = null;
        String prompt = "";
        List<Call> calls = new ArrayList<>();
        String detail = "Comparison inputs unavailable. No score was assigned.";
        try {
            frozen = outcomes.find(incidentId, key.terminalStatus());
            var expectation =
                    ConfidenceExpectation.evaluate(key.answerKey(), frozen.evidence(), frozen.reportAttempt());
            TextScores text = ComparisonRubric.nullScores(key.answerKey(), frozen.report());
            if (text == null) {
                prompt = prompt(key, frozen);
                for (int attempt = 0; attempt < 2; attempt++) {
                    Instant started = clock.instant();
                    TextJudge.Reply reply = null;
                    try {
                        reply = judge.evaluate(prompt);
                        text = TextScores.parse(mapper, reply.content());
                        calls.add(new Call(
                                started, clock.instant(), reply.content(), reply.providerResponse(), "AVAILABLE"));
                        break;
                    } catch (RuntimeException exception) {
                        calls.add(new Call(
                                started,
                                clock.instant(),
                                reply == null ? null : reply.content(),
                                reply == null ? null : reply.providerResponse(),
                                "FAILED"));
                        if (Thread.currentThread().isInterrupted()) break;
                    }
                }
            }
            if (text != null) {
                grade = ComparisonRubric.grade(key.answerKey(), frozen.report(), frozen.outcome(), text, expectation);
                detail =
                        "Text match is AI-assessed; exact matches, averages and decision scoring use the fixed rubric.";
            } else detail = "Text evaluator failed after automatic retry. No score was assigned.";
        } catch (RuntimeException exception) {
            detail = "Comparison inputs unavailable. No score was assigned.";
        }
        var response = new ComparisonResponse(
                id,
                incidentId,
                grade == null ? "UNAVAILABLE" : "AVAILABLE",
                detail,
                requestedAt,
                clock.instant(),
                frozen == null ? null : frozen.investigationId(),
                frozen == null ? null : frozen.reportAttemptId(),
                frozen == null ? null : frozen.decisionId(),
                calls.isEmpty() ? "not-used" : properties.model(),
                PROMPT_VERSION,
                ComparisonRubric.VERSION,
                key.oracleVersion(),
                grade,
                frozen == null ? null : frozen.report());
        Input input = new Input(tenantId, operatorId, key, frozen);
        store.save(
                tenantId,
                new Artifact(
                        input,
                        ComparisonArtifactStore.hash(mapper.writeValueAsString(input)),
                        prompt,
                        ComparisonArtifactStore.hash(prompt),
                        calls,
                        response,
                        ComparisonArtifactStore.hash(mapper.writeValueAsString(response))));
        return response;
    }

    private String prompt(AnswerKeyRevealResponse key, FrozenOutcome frozen) {
        try {
            String instructions =
                    new ClassPathResource("comparison/text-prompt-v1.txt").getContentAsString(StandardCharsets.UTF_8);
            // Deliberately exclude disposition, confidence and all human decision fields.
            String data = mapper.writeValueAsString(Map.of(
                    "expectedRootCause",
                    key.answerKey().rootCause(),
                    "expectedRecommendation",
                    key.answerKey().recommendation(),
                    "requiredEvidence",
                    key.answerKey().requiredEvidence(),
                    "actualRootCause",
                    frozen.report().get("probableCause"),
                    "actualRecommendation",
                    frozen.report().get("recommendation"),
                    "observedEvidence",
                    frozen.evidence()));
            return instructions + "\nCOMPARISON_DATA_JSON\n" + data;
        } catch (IOException exception) {
            throw new IllegalStateException("Comparison prompt unavailable", exception);
        }
    }

    record Input(UUID tenantId, UUID operatorId, AnswerKeyRevealResponse answerKey, FrozenOutcome outcomes) {}

    record Call(Instant startedAt, Instant completedAt, String output, String providerResponse, String status) {}

    record Artifact(
            Input input,
            String inputHash,
            String prompt,
            String promptHash,
            List<Call> calls,
            ComparisonResponse response,
            String responseHash) {}
}
