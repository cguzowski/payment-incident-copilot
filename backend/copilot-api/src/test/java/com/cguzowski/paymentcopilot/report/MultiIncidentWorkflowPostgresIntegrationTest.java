package com.cguzowski.paymentcopilot.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cguzowski.paymentcopilot.knowledge.catalog.KnowledgeEmbedding;
import com.cguzowski.paymentcopilot.knowledge.catalog.KnowledgeEmbeddingClient;
import com.cguzowski.paymentcopilot.knowledge.catalog.KnowledgeIngestionService;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class MultiIncidentWorkflowPostgresIntegrationTest {
    private static final UUID TENANT = UUID.fromString("8b860d80-d17f-4e6b-8c48-af35f26a4d61");
    private static final UUID OPERATOR = UUID.fromString("7b636625-53d1-46f7-92a9-9c8c27a243d1");
    private static final Instant DETECTED = Instant.parse("2026-09-30T12:00:00Z");

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("pgvector/pgvector:pg17")
            .withDatabaseName("payment_copilot")
            .withUsername("payment_copilot")
            .withPassword("test_only_password");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    WebApplicationContext context;

    @Autowired
    JdbcClient jdbc;

    @Autowired
    JsonMapper mapper;

    @Autowired
    KnowledgeIngestionService ingestion;

    @MockitoBean
    McpSyncClient mcp;

    @MockitoBean
    KnowledgeEmbeddingClient embeddings;

    @MockitoBean
    ReportModel model;

    MockMvc http;

    @BeforeEach
    void prepare() {
        http = MockMvcBuilders.webAppContextSetup(context).build();
        for (String table : List.of(
                "human_decision",
                "report_claim_knowledge_reference",
                "report_claim_evidence_reference",
                "report_claim",
                "report_generation_attempt",
                "knowledge_retrieval_result",
                "knowledge_retrieval_attempt",
                "knowledge_chunk",
                "knowledge_document_version",
                "evidence_collection_attempt",
                "investigation",
                "incident")) {
            jdbc.sql("DELETE FROM " + table).update();
        }
        float[] vector = new float[768];
        vector[0] = 1;
        when(embeddings.embed(anyString())).thenReturn(new KnowledgeEmbedding("nomic-embed-text", 768, true, vector));
        assertThat(ingestion.importApprovedSources().importedDocuments()).isEqualTo(14);
        when(mcp.isInitialized()).thenReturn(true);
        when(model.modelId()).thenReturn("deterministic-workflow-test");
    }

    @ParameterizedTest
    @ValueSource(
            strings = {"S301", "S302", "S303", "S304", "S305", "S306", "S311", "S312", "S313", "S314", "S315", "S316"})
    void triagesEachFamilyWithExactSourcesAndHumanDecision(String code) throws Exception {
        JsonNode scenario;
        try (var stream = new ClassPathResource("multi-incidents/v1/catalog.json").getInputStream()) {
            scenario = mapper.readTree(stream)
                    .get("scenarios")
                    .valueStream()
                    .filter(value -> value.get("code").asText().equals(code))
                    .findFirst()
                    .orElseThrow();
        }
        String family = scenario.get("incidentType").asText();
        String reference = "sig-v1-" + code + "-1790769600-1234567890ab";
        JsonNode alert = response(http.perform(mutate("/api/alerts")
                        .content(mapper.writeValueAsString(Map.of(
                                "externalAlertId",
                                reference,
                                "incidentType",
                                family,
                                "severity",
                                "HIGH",
                                "detectedAt",
                                DETECTED.toString(),
                                "title",
                                scenario.get("title").asText(),
                                "description",
                                scenario.get("description").asText()))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString());
        String incident = alert.get("incidentId").asText();
        assertThat(alert.get("incidentType").asText()).isEqualTo(family);
        JsonNode investigation = response(http.perform(mutate("/api/incidents/" + incident + "/investigations"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString());
        String id = investigation.get("investigationId").asText();
        JsonNode fixture = scenario.get("evidence");
        String availability = fixture.get("availability").asText();
        when(mcp.callTool(any())).thenAnswer(invocation -> {
            CallToolRequest request = invocation.getArgument(0);
            assertThat(request.arguments())
                    .containsEntry("scenarioReference", reference)
                    .containsEntry("tenantId", TENANT.toString());
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("sourceSystem", "synthetic-observability");
            result.put("sourceTool", "getRecentServiceErrors");
            result.put("retrievedAt", DETECTED.plusSeconds(1).toString());
            result.put("status", availability);
            result.put("contentSchemaVersion", "service-errors/v1");
            result.put("toolCallId", request.arguments().get("toolCallId"));
            result.put("correlationId", request.arguments().get("correlationId"));
            if (fixture.has("statusDetail"))
                result.put("statusDetail", fixture.get("statusDetail").asText());
            if (fixture.has("serviceName")) {
                List<Map<String, Object>> errors = new ArrayList<>();
                int ordinal = 0;
                for (JsonNode error : fixture.get("errors"))
                    errors.add(Map.of(
                            "sourceEventId",
                            reference + "-e" + (++ordinal),
                            "observedAt",
                            DETECTED.minusSeconds(
                                            error.get("secondsBeforeDetection").asInt())
                                    .toString(),
                            "errorCode",
                            error.get("errorCode").asText(),
                            "count",
                            error.get("count").asInt()));
                result.put(
                        "content",
                        Map.of(
                                "serviceName",
                                fixture.get("serviceName").asText(),
                                "observedFrom",
                                DETECTED.minusSeconds(300).toString(),
                                "observedTo",
                                DETECTED.toString(),
                                "errors",
                                errors));
            }
            return new CallToolResult(List.of(), false, result, Map.of());
        });
        JsonNode evidence = response(http.perform(mutate("/api/investigations/" + id + "/evidence-collections"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(availability))
                .andReturn()
                .getResponse()
                .getContentAsString());
        JsonNode retrieval = response(http.perform(mutate("/api/investigations/" + id + "/knowledge-retrievals"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("AVAILABLE"))
                .andReturn()
                .getResponse()
                .getContentAsString());
        assertThat(retrieval.get("metadataFilters").get("incidentFamily").asText())
                .isEqualTo(family);
        assertThat(retrieval.get("results").size()).isGreaterThanOrEqualTo(2);
        assertThat(jdbc.sql(
                                "SELECT DISTINCT incident_family FROM knowledge_document_version JOIN knowledge_chunk ON knowledge_chunk.document_version_id = knowledge_document_version.id JOIN knowledge_retrieval_result ON knowledge_retrieval_result.chunk_id = knowledge_chunk.id")
                        .query(String.class)
                        .list())
                .containsExactly(family);
        List<String> types = retrieval
                .get("results")
                .valueStream()
                .map(value -> value.get("documentType").asText())
                .toList();
        assertThat(types).contains("RUNBOOK", "POLICY");
        for (JsonNode chunk : retrieval.get("results")) {
            assertThat(chunk.get("sourceName").asText()).startsWith("knowledge/multi-incidents/v1/");
            assertThat(chunk.get("sourceStartLine").asInt()).isPositive();
        }
        String evidenceId = evidence.get("evidenceId").asText();
        String chunkId = retrieval.get("results").get(0).get("chunkId").asText();
        boolean degraded = !availability.equals("AVAILABLE");
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("disposition", degraded ? "INSUFFICIENT_EVIDENCE" : "PROPOSED");
        report.put("summary", claim("Synthetic " + family + " investigation", evidenceId, null));
        report.put(
                "observations",
                degraded
                        ? List.of()
                        : List.of(claim(
                                "sourceEventId=" + reference + "-e1"
                                        + "; observedAt="
                                        + DETECTED.minusSeconds(fixture.get("errors")
                                                .get(0)
                                                .get("secondsBeforeDetection")
                                                .asInt())
                                        + "; errorCode="
                                        + fixture.get("errors")
                                                .get(0)
                                                .get("errorCode")
                                                .asText()
                                        + "; count="
                                        + fixture.get("errors")
                                                .get(0)
                                                .get("count")
                                                .asLong(),
                                evidenceId,
                                null)));
        report.put("inferences", List.of());
        report.put(
                "probableCause",
                degraded
                        ? null
                        : claim("Affected stage dependency requires independent confirmation", evidenceId, chunkId));
        report.put(
                "recommendation",
                degraded
                        ? null
                        : claim("Ask the service owner to verify the missing confirming records", evidenceId, chunkId));
        report.put(
                "confidence",
                Map.of(
                        "level",
                        degraded ? "LOW" : "MEDIUM",
                        "rationale",
                        "Aggregate evidence has bounded scope",
                        "evidenceIds",
                        List.of(evidenceId)));
        report.put("contradictions", List.of());
        report.put("evidenceGaps", List.of(Map.of("description", "Independent final-state records are unavailable")));
        String reportJson = mapper.writeValueAsString(report);
        when(model.generate(any(), any())).thenAnswer(invocation -> {
            assertThat(invocation.<String>getArgument(0)).contains(family, evidenceId, chunkId);
            return new ReportModelResponse(reportJson, "synthetic-provider-request");
        });
        JsonNode generated = response(http.perform(mutate("/api/investigations/" + id + "/reports"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("AVAILABLE"))
                .andReturn()
                .getResponse()
                .getContentAsString());
        assertThat(generated.get("latestEvidenceId").asText()).isEqualTo(evidenceId);
        assertThat(generated.get("retrievalId").asText())
                .isEqualTo(retrieval.get("retrievalId").asText());
        assertThat(generated.get("report").get("disposition").asText())
                .isEqualTo(degraded ? "INSUFFICIENT_EVIDENCE" : "PROPOSED");
        if (degraded) {
            assertThat(generated.get("report").get("probableCause").isNull()).isTrue();
            assertThat(generated.get("report").get("recommendation").isNull()).isTrue();
            assertThat(generated.get("report").get("confidence").get("level").asText())
                    .isEqualTo("LOW");
        }
        assertThat(jdbc.sql("SELECT COUNT(*) FROM human_decision")
                        .query(Integer.class)
                        .single())
                .isZero();
        String outcome = degraded ? "REJECTED" : "APPROVED";
        http.perform(mutate("/api/investigations/" + id + "/decisions")
                        .content(mapper.writeValueAsString(
                                Map.of("outcome", outcome, "reason", "Deterministic human-review test"))))
                .andExpect(status().isCreated());
        http.perform(get("/api/incidents/" + incident).header("X-Synthetic-Tenant-Id", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(outcome));
        JsonNode timeline = response(
                http.perform(get("/api/investigations/" + id + "/timeline").header("X-Synthetic-Tenant-Id", TENANT))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString());
        assertThat(timeline.valueStream()
                        .map(value -> value.get("eventType").asText())
                        .toList())
                .contains("EVIDENCE_COLLECTION", "KNOWLEDGE_RETRIEVAL", "REPORT_GENERATION", "HUMAN_DECISION");
        http.perform(get("/api/investigations/" + id + "/timeline").header("X-Synthetic-Tenant-Id", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    private JsonNode response(String value) {
        return mapper.readTree(value);
    }

    private MockHttpServletRequestBuilder mutate(String path) {
        return post(path)
                .header("X-Synthetic-Tenant-Id", TENANT)
                .header("X-Synthetic-Operator-Id", OPERATOR)
                .contentType(MediaType.APPLICATION_JSON);
    }

    private Map<String, Object> claim(String statement, String evidence, String knowledge) {
        return Map.of(
                "statement",
                statement,
                "evidenceIds",
                List.of(evidence),
                "knowledgeChunkIds",
                knowledge == null ? List.of() : List.of(knowledge));
    }
}
