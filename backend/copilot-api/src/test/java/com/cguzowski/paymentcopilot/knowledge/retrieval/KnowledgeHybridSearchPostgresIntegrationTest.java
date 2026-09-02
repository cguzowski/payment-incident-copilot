package com.cguzowski.paymentcopilot.knowledge.retrieval;

import static org.assertj.core.api.Assertions.assertThat;

import com.cguzowski.paymentcopilot.knowledge.catalog.KnowledgeApprovalStatus;
import com.cguzowski.paymentcopilot.knowledge.catalog.KnowledgeDocumentType;
import com.cguzowski.paymentcopilot.knowledge.catalog.KnowledgeEmbeddingClient;
import com.cguzowski.paymentcopilot.knowledge.catalog.KnowledgeSourceFormat;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class KnowledgeHybridSearchPostgresIntegrationTest {

    private static final UUID TENANT_ID = UUID.fromString("8b860d80-d17f-4e6b-8c48-af35f26a4d61");
    private static final UUID OTHER_TENANT_ID = UUID.fromString("076a18a3-d54f-486a-b3ec-189e1048fd28");

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("pgvector/pgvector:pg17")
            .withDatabaseName("payment_copilot")
            .withUsername("payment_copilot")
            .withPassword("test_only_password");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private JdbcClient jdbcClient;

    @Autowired
    private KnowledgeSearchRepository repository;

    @BeforeEach
    void setUpCorpus() {
        clearCorpus();

        insertDocumentAndChunk(
                TENANT_ID,
                "11111111-1111-4111-8111-111111111111",
                "21111111-1111-4111-8111-111111111111",
                KnowledgeDocumentType.RUNBOOK,
                KnowledgeApprovalStatus.APPROVED,
                "AUTHORIZATION_DECLINE_RATE_SPIKE",
                "Gateway Failures > Diagnosis",
                "Inspect GATEWAY_TIMEOUT and connection reset observations.",
                unitVector(0));
        insertDocumentAndChunk(
                TENANT_ID,
                "12222222-2222-4222-8222-222222222222",
                "22222222-2222-4222-8222-222222222222",
                KnowledgeDocumentType.POLICY,
                KnowledgeApprovalStatus.APPROVED,
                "AUTHORIZATION_DECLINE_RATE_SPIKE",
                "Human authority",
                "Authorization incident reports require human review.",
                normalizedVector(0.8f, 0.6f));
        insertDocumentAndChunk(
                OTHER_TENANT_ID,
                "13333333-3333-4333-8333-333333333333",
                "23333333-3333-4333-8333-333333333333",
                KnowledgeDocumentType.RUNBOOK,
                KnowledgeApprovalStatus.APPROVED,
                "AUTHORIZATION_DECLINE_RATE_SPIKE",
                "Gateway Failures",
                "GATEWAY_TIMEOUT must never cross tenant boundaries.",
                unitVector(0));
        insertDocumentAndChunk(
                TENANT_ID,
                "14444444-4444-4444-8444-444444444444",
                "24444444-4444-4444-8444-444444444444",
                KnowledgeDocumentType.RUNBOOK,
                KnowledgeApprovalStatus.DRAFT,
                "AUTHORIZATION_DECLINE_RATE_SPIKE",
                "Draft guidance",
                "GATEWAY_TIMEOUT draft text must not be retrieved.",
                unitVector(0));
        insertDocumentAndChunk(
                TENANT_ID,
                "15555555-5555-4555-8555-555555555555",
                "25555555-5555-4555-8555-555555555555",
                KnowledgeDocumentType.RUNBOOK,
                KnowledgeApprovalStatus.APPROVED,
                "UNRELATED_INCIDENT_FAMILY",
                "Unrelated guidance",
                "GATEWAY_TIMEOUT unrelated text must not be retrieved.",
                unitVector(0));
        insertDocumentAndChunk(
                TENANT_ID,
                "16666666-6666-4666-8666-666666666666",
                "26666666-6666-4666-8666-666666666666",
                KnowledgeDocumentType.POLICY,
                KnowledgeApprovalStatus.SUPERSEDED,
                "AUTHORIZATION_DECLINE_RATE_SPIKE",
                "Superseded policy",
                "Zebra protocol automatic approval superseded text must not be retrieved.",
                unitVector(0));
        insertDocumentAndChunk(
                TENANT_ID,
                "17777777-7777-4777-8777-777777777777",
                "27777777-7777-4777-8777-777777777777",
                KnowledgeDocumentType.RUNBOOK,
                KnowledgeApprovalStatus.APPROVED,
                "AUTHORIZATION_DECLINE_RATE_SPIKE",
                "Historical provider",
                "Historical guidance with no matching query terms.",
                vectorLiteral(unitVectorArray(1024, 0)),
                "amazon.titan-embed-text-v2:0",
                1024);
        insertUnembeddedPdfDocumentAndChunk();
    }

    @Test
    void scoresOnlyVectorsFromTheQueryModelAndDimensionWhileKeepingLexicalFallback() {
        KnowledgeSearchRequest request = new KnowledgeSearchRequest(
                TENANT_ID,
                "AUTHORIZATION_DECLINE_RATE_SPIKE",
                Instant.parse("2026-08-28T10:00:00Z"),
                "gateway timeout authorization",
                KnowledgeEmbeddingClient.MODEL_ID,
                KnowledgeEmbeddingClient.DIMENSIONS,
                unitVectorArray(0),
                20,
                60,
                0.0f,
                0.55f);

        List<KnowledgeSearchCandidate> candidates = repository.search(request);

        assertThat(candidates).hasSize(3);
        KnowledgeSearchCandidate gateway = candidates.stream()
                .filter(candidate -> candidate.rawContent().contains("GATEWAY_TIMEOUT"))
                .findFirst()
                .orElseThrow();
        assertThat(gateway.lexicalPosition()).isEqualTo(1);
        assertThat(gateway.vectorPosition()).isEqualTo(1);
        assertThat(gateway.vectorSimilarity()).isEqualTo(1.0f);
        KnowledgeSearchCandidate historical = candidates.stream()
                .filter(candidate ->
                        candidate.chunkId().equals(UUID.fromString("27777777-7777-4777-8777-777777777777")))
                .findFirst()
                .orElseThrow();
        assertThat(gateway.fusedScore()).isGreaterThan(historical.fusedScore());
        assertThat(historical.lexicalPosition()).isNotNull();
        assertThat(historical.vectorPosition()).isNull();
        assertThat(historical.vectorSimilarity()).isNull();
        assertThat(candidates).allSatisfy(candidate -> {
            assertThat(candidate.tenantId()).isEqualTo(TENANT_ID);
            assertThat(candidate.approvalStatus()).isEqualTo(KnowledgeApprovalStatus.APPROVED);
            assertThat(candidate.incidentFamily()).isEqualTo("AUTHORIZATION_DECLINE_RATE_SPIKE");
        });
    }

    @Test
    void retrievesApprovedUnembeddedPdfLexicallyAndExcludesSupersededExactMatch() {
        KnowledgeSearchRequest request = new KnowledgeSearchRequest(
                TENANT_ID,
                "AUTHORIZATION_DECLINE_RATE_SPIKE",
                Instant.parse("2026-08-28T10:00:00Z"),
                "zebra protocol automatic approval",
                KnowledgeEmbeddingClient.MODEL_ID,
                KnowledgeEmbeddingClient.DIMENSIONS,
                unitVectorArray(0),
                20,
                60,
                0.0f,
                0.55f);

        List<KnowledgeSearchCandidate> candidates = repository.search(request);

        KnowledgeSearchCandidate pdf = candidates.stream()
                .filter(candidate ->
                        candidate.chunkId().equals(UUID.fromString("28888888-8888-4888-8888-888888888888")))
                .findFirst()
                .orElseThrow();
        assertThat(pdf.sourceName()).isEqualTo("rb-zebra-provider-isolation.pdf");
        assertThat(pdf.sourceFormat()).isEqualTo(KnowledgeSourceFormat.PDF);
        assertThat(pdf.pdfSha256()).isEqualTo("d".repeat(64));
        assertThat(pdf.sourceStartLine()).isNull();
        assertThat(pdf.sourceEndLine()).isNull();
        assertThat(pdf.sourceStartPage()).isEqualTo(2);
        assertThat(pdf.sourceEndPage()).isEqualTo(2);
        assertThat(pdf.sourceStartBlock()).isEqualTo(3);
        assertThat(pdf.sourceEndBlock()).isEqualTo(5);
        assertThat(pdf.lexicalPosition()).isNotNull();
        assertThat(pdf.vectorPosition()).isNull();
        assertThat(pdf.vectorSimilarity()).isNull();
        assertThat(candidates)
                .extracting(KnowledgeSearchCandidate::chunkId)
                .doesNotContain(UUID.fromString("26666666-6666-4666-8666-666666666666"));
    }

    @Test
    void appliesCandidateDepthSeparatelyToEachDocumentTypeAndModality() {
        clearCorpus();

        insertBalancedCandidate(
                KnowledgeDocumentType.RUNBOOK, "runbook-lexical-1", "zebra exact signal", unitVector(1));
        insertBalancedCandidate(
                KnowledgeDocumentType.RUNBOOK, "runbook-lexical-2", "zebra exact signal", unitVector(1));
        insertBalancedCandidate(KnowledgeDocumentType.RUNBOOK, "runbook-vector-1", "unrelated guidance", unitVector(0));
        insertBalancedCandidate(KnowledgeDocumentType.RUNBOOK, "runbook-vector-2", "unrelated guidance", unitVector(0));
        insertBalancedCandidate(KnowledgeDocumentType.POLICY, "policy-lexical-1", "zebra exact signal", unitVector(1));
        insertBalancedCandidate(KnowledgeDocumentType.POLICY, "policy-lexical-2", "zebra exact signal", unitVector(1));
        insertBalancedCandidate(KnowledgeDocumentType.POLICY, "policy-vector-1", "unrelated guidance", unitVector(0));
        insertBalancedCandidate(KnowledgeDocumentType.POLICY, "policy-vector-2", "unrelated guidance", unitVector(0));

        KnowledgeSearchRequest request = new KnowledgeSearchRequest(
                TENANT_ID,
                "AUTHORIZATION_DECLINE_RATE_SPIKE",
                Instant.parse("2026-08-28T10:00:00Z"),
                "zebra exact signal",
                KnowledgeEmbeddingClient.MODEL_ID,
                KnowledgeEmbeddingClient.DIMENSIONS,
                unitVectorArray(0),
                2,
                60,
                0.0f,
                0.55f);

        List<KnowledgeSearchCandidate> candidates = repository.search(request);

        assertThat(candidates).hasSize(8);
        assertThat(candidates)
                .filteredOn(candidate -> candidate.documentType() == KnowledgeDocumentType.RUNBOOK)
                .hasSize(4);
        assertThat(candidates)
                .filteredOn(candidate -> candidate.documentType() == KnowledgeDocumentType.POLICY)
                .hasSize(4);
        assertThat(candidates)
                .filteredOn(candidate -> candidate.lexicalPosition() != null)
                .extracting(KnowledgeSearchCandidate::lexicalPosition)
                .containsOnly(1, 2);
        assertThat(candidates)
                .filteredOn(candidate -> candidate.vectorPosition() != null)
                .extracting(KnowledgeSearchCandidate::vectorPosition)
                .containsOnly(1, 2);
    }

    private void clearCorpus() {
        jdbcClient.sql("DELETE FROM knowledge_retrieval_result").update();
        jdbcClient.sql("DELETE FROM knowledge_retrieval_attempt").update();
        jdbcClient.sql("DELETE FROM knowledge_chunk").update();
        jdbcClient.sql("DELETE FROM knowledge_document_version").update();
    }

    private void insertBalancedCandidate(KnowledgeDocumentType type, String key, String rawContent, String vector) {
        insertDocumentAndChunk(
                TENANT_ID,
                UUID.nameUUIDFromBytes((key + "-version").getBytes()).toString(),
                UUID.nameUUIDFromBytes((key + "-chunk").getBytes()).toString(),
                type,
                KnowledgeApprovalStatus.APPROVED,
                "AUTHORIZATION_DECLINE_RATE_SPIKE",
                key,
                rawContent,
                vector);
    }

    private void insertDocumentAndChunk(
            UUID tenantId,
            String documentVersionId,
            String chunkId,
            KnowledgeDocumentType type,
            KnowledgeApprovalStatus status,
            String incidentFamily,
            String sectionPath,
            String rawContent,
            String vector) {
        insertDocumentAndChunk(
                tenantId,
                documentVersionId,
                chunkId,
                type,
                status,
                incidentFamily,
                sectionPath,
                rawContent,
                vector,
                KnowledgeEmbeddingClient.MODEL_ID,
                KnowledgeEmbeddingClient.DIMENSIONS);
    }

    private void insertDocumentAndChunk(
            UUID tenantId,
            String documentVersionId,
            String chunkId,
            KnowledgeDocumentType type,
            KnowledgeApprovalStatus status,
            String incidentFamily,
            String sectionPath,
            String rawContent,
            String vector,
            String modelId,
            int dimensions) {
        UUID versionId = UUID.fromString(documentVersionId);
        jdbcClient
                .sql("""
                        INSERT INTO knowledge_document_version (
                            id, tenant_id, document_id, document_type, title,
                            document_version, incident_family, applies_to,
                            approval_status, approved_by, approved_at, effective_at,
                            source_name, source_content_hash, source_format,
                            source_artifact_hash, pdf_artifact_hash,
                            extraction_strategy_version, imported_at
                        ) VALUES (
                            :id, :tenantId, :documentId, :type, :title,
                            '1.0.0', :incidentFamily, 'Card authorization',
                            :status, :approvedBy,
                            TIMESTAMPTZ '2026-08-20 10:00:00Z',
                            TIMESTAMPTZ '2026-08-21 00:00:00Z',
                            :sourceName, :hash, 'MARKDOWN', :hash, NULL,
                            'markdown-front-matter/v1',
                            TIMESTAMPTZ '2026-08-28 09:00:00Z'
                        )
                        """)
                .param("id", versionId)
                .param("tenantId", tenantId)
                .param("documentId", UUID.nameUUIDFromBytes(documentVersionId.getBytes()))
                .param("type", type.name())
                .param(
                        "title",
                        type == KnowledgeDocumentType.RUNBOOK
                                ? "Authorization Decline Runbook"
                                : "Synthetic Payment Incident Response Policy")
                .param("incidentFamily", incidentFamily)
                .param("status", status.name())
                .param("approvedBy", UUID.fromString("7b636625-53d1-46f7-92a9-9c8c27a243d1"))
                .param("sourceName", chunkId + ".md")
                .param("hash", "a".repeat(64))
                .update();
        jdbcClient
                .sql("""
                        INSERT INTO knowledge_chunk (
                            id, tenant_id, document_version_id, chunk_ordinal,
                            section_path, raw_content, embedding_input,
                            raw_content_hash, embedding_input_hash,
                            embedding_input_template_version, chunking_strategy_version,
                            source_start_line, source_end_line, estimated_tokens,
                            embedding_model_id, embedding_dimensions,
                            embedding_normalized, embedded_at, embedding
                        ) VALUES (
                            :id, :tenantId, :documentVersionId, 0,
                            :sectionPath, :rawContent, :embeddingInput,
                            :rawHash, :embeddingHash, 'embedding-input/v1',
                            'markdown-sections/v1', 20, 20, 10,
                            :modelId, :dimensions, TRUE,
                            TIMESTAMPTZ '2026-08-28 09:00:00Z', CAST(:embedding AS vector)
                        )
                        """)
                .param("id", UUID.fromString(chunkId))
                .param("tenantId", tenantId)
                .param("documentVersionId", versionId)
                .param("sectionPath", sectionPath)
                .param("rawContent", rawContent)
                .param(
                        "embeddingInput",
                        "Document: fixture\nSection: " + sectionPath + "\nType: " + type
                                + "\nApplies to: Card authorization\n\n" + rawContent)
                .param("rawHash", "b".repeat(64))
                .param("embeddingHash", "c".repeat(64))
                .param("modelId", modelId)
                .param("dimensions", dimensions)
                .param("embedding", vector)
                .update();
    }

    private void insertUnembeddedPdfDocumentAndChunk() {
        UUID documentVersionId = UUID.fromString("18888888-8888-4888-8888-888888888888");
        jdbcClient
                .sql("""
                        INSERT INTO knowledge_document_version (
                            id, tenant_id, document_id, document_type, title,
                            document_version, incident_family, applies_to,
                            approval_status, approved_by, approved_at, effective_at,
                            source_name, source_content_hash, source_format,
                            source_artifact_hash, pdf_artifact_hash,
                            extraction_strategy_version, imported_at
                        ) VALUES (
                            :id, :tenantId, :documentId, 'RUNBOOK',
                            'Zebra Provider Isolation Runbook', '1.0.0',
                            'AUTHORIZATION_DECLINE_RATE_SPIKE', 'Provider isolation',
                            'APPROVED', :approvedBy,
                            TIMESTAMPTZ '2026-08-20 10:00:00Z',
                            TIMESTAMPTZ '2026-08-21 00:00:00Z',
                            'rb-zebra-provider-isolation.pdf', :contentHash, 'PDF',
                            :sourceHash, :pdfHash, 'pdfbox-text-pages/v1',
                            TIMESTAMPTZ '2026-08-28 09:00:00Z'
                        )
                        """)
                .param("id", documentVersionId)
                .param("tenantId", TENANT_ID)
                .param("documentId", UUID.fromString("38888888-8888-4888-8888-888888888888"))
                .param("approvedBy", UUID.fromString("7b636625-53d1-46f7-92a9-9c8c27a243d1"))
                .param("contentHash", "e".repeat(64))
                .param("sourceHash", "a".repeat(64))
                .param("pdfHash", "d".repeat(64))
                .update();
        jdbcClient
                .sql("""
                        INSERT INTO knowledge_chunk (
                            id, tenant_id, document_version_id, chunk_ordinal,
                            section_path, raw_content, embedding_input,
                            raw_content_hash, embedding_input_hash,
                            embedding_input_template_version, chunking_strategy_version,
                            source_start_line, source_end_line,
                            source_start_page, source_end_page,
                            source_start_block, source_end_block, estimated_tokens,
                            embedding_model_id, embedding_dimensions,
                            embedding_normalized, embedded_at, embedding
                        ) VALUES (
                            :id, :tenantId, :documentVersionId, 0,
                            'Provider isolation',
                            'Zebra protocol requires provider isolation before manual review.',
                            'Document: Zebra Provider Isolation Runbook',
                            :rawHash, :embeddingHash,
                            'embedding-input/v1', 'pdf-page-sections/v1',
                            NULL, NULL, 2, 2, 3, 5, 9,
                            NULL, NULL, NULL, NULL, NULL
                        )
                        """)
                .param("id", UUID.fromString("28888888-8888-4888-8888-888888888888"))
                .param("tenantId", TENANT_ID)
                .param("documentVersionId", documentVersionId)
                .param("rawHash", "b".repeat(64))
                .param("embeddingHash", "c".repeat(64))
                .update();
    }

    private static String unitVector(int index) {
        return vectorLiteral(unitVectorArray(index));
    }

    private static float[] unitVectorArray(int index) {
        return unitVectorArray(KnowledgeEmbeddingClient.DIMENSIONS, index);
    }

    private static float[] unitVectorArray(int dimensions, int index) {
        float[] vector = new float[dimensions];
        vector[index] = 1.0f;
        return vector;
    }

    private static String normalizedVector(float first, float second) {
        float[] vector = new float[KnowledgeEmbeddingClient.DIMENSIONS];
        vector[0] = first;
        vector[1] = second;
        return vectorLiteral(vector);
    }

    private static String vectorLiteral(float[] vector) {
        StringBuilder value = new StringBuilder("[");
        for (int index = 0; index < vector.length; index++) {
            if (index > 0) {
                value.append(',');
            }
            value.append(Float.toString(vector[index]));
        }
        return value.append(']').toString();
    }
}
