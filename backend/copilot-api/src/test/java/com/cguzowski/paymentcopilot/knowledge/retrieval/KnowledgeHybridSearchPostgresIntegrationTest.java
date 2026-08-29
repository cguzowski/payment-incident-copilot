package com.cguzowski.paymentcopilot.knowledge.retrieval;

import static org.assertj.core.api.Assertions.assertThat;

import com.cguzowski.paymentcopilot.knowledge.catalog.KnowledgeApprovalStatus;
import com.cguzowski.paymentcopilot.knowledge.catalog.KnowledgeDocumentType;
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
        jdbcClient.sql("DELETE FROM knowledge_retrieval_result").update();
        jdbcClient.sql("DELETE FROM knowledge_retrieval_attempt").update();
        jdbcClient.sql("DELETE FROM knowledge_chunk").update();
        jdbcClient.sql("DELETE FROM knowledge_document_version").update();

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
                "GATEWAY_TIMEOUT superseded text must not be retrieved.",
                unitVector(0));
    }

    @Test
    void filtersBeforeIndependentRankingAndReturnsDeterministicRrfCandidates() {
        KnowledgeSearchRequest request = new KnowledgeSearchRequest(
                TENANT_ID,
                "AUTHORIZATION_DECLINE_RATE_SPIKE",
                Instant.parse("2026-08-28T10:00:00Z"),
                "gateway timeout authorization",
                unitVectorArray(0),
                20,
                60,
                0.0f,
                0.55f);

        List<KnowledgeSearchCandidate> candidates = repository.search(request);

        assertThat(candidates).hasSize(2);
        assertThat(candidates)
                .extracting(KnowledgeSearchCandidate::documentType)
                .containsExactly(KnowledgeDocumentType.RUNBOOK, KnowledgeDocumentType.POLICY);
        assertThat(candidates.getFirst().rawContent()).contains("GATEWAY_TIMEOUT");
        assertThat(candidates.getFirst().lexicalPosition()).isEqualTo(1);
        assertThat(candidates.getFirst().vectorPosition()).isEqualTo(1);
        assertThat(candidates.getFirst().vectorSimilarity()).isEqualTo(1.0f);
        assertThat(candidates.getFirst().fusedScore())
                .isGreaterThan(candidates.getLast().fusedScore());
        assertThat(candidates).allSatisfy(candidate -> {
            assertThat(candidate.tenantId()).isEqualTo(TENANT_ID);
            assertThat(candidate.approvalStatus()).isEqualTo(KnowledgeApprovalStatus.APPROVED);
            assertThat(candidate.incidentFamily()).isEqualTo("AUTHORIZATION_DECLINE_RATE_SPIKE");
        });
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
        UUID versionId = UUID.fromString(documentVersionId);
        jdbcClient
                .sql("""
                        INSERT INTO knowledge_document_version (
                            id, tenant_id, document_id, document_type, title,
                            document_version, incident_family, applies_to,
                            approval_status, approved_by, approved_at, effective_at,
                            source_name, source_content_hash, imported_at
                        ) VALUES (
                            :id, :tenantId, :documentId, :type, :title,
                            '1.0.0', :incidentFamily, 'Card authorization',
                            :status, :approvedBy,
                            TIMESTAMPTZ '2026-08-20 10:00:00Z',
                            TIMESTAMPTZ '2026-08-21 00:00:00Z',
                            :sourceName, :hash, TIMESTAMPTZ '2026-08-28 09:00:00Z'
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
                            'amazon.titan-embed-text-v2:0', 1024, TRUE,
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
                .param("embedding", vector)
                .update();
    }

    private static String unitVector(int index) {
        return vectorLiteral(unitVectorArray(index));
    }

    private static float[] unitVectorArray(int index) {
        float[] vector = new float[1024];
        vector[index] = 1.0f;
        return vector;
    }

    private static String normalizedVector(float first, float second) {
        float[] vector = new float[1024];
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
