package com.cguzowski.paymentcopilot.knowledge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
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
class KnowledgeSchemaPostgresIntegrationTest {

    private static final UUID TENANT_ID = UUID.fromString("8b860d80-d17f-4e6b-8c48-af35f26a4d61");
    private static final UUID OTHER_TENANT_ID = UUID.fromString("076a18a3-d54f-486a-b3ec-189e1048fd28");
    private static final UUID DOCUMENT_ID = UUID.fromString("66a84fed-3d77-4e7e-9a1b-e25ff37e2280");
    private static final UUID DOCUMENT_VERSION_ID = UUID.fromString("a37cd2fa-a938-4e4e-b7f3-956f8d293f28");
    private static final UUID CHUNK_ID = UUID.fromString("b0f6a021-e6d3-4f05-bbad-7a2cb7652714");

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

    @BeforeEach
    void cleanKnowledge() {
        jdbcClient.sql("DELETE FROM knowledge_retrieval_result").update();
        jdbcClient.sql("DELETE FROM knowledge_retrieval_attempt").update();
        jdbcClient.sql("DELETE FROM knowledge_chunk").update();
        jdbcClient.sql("DELETE FROM knowledge_document_version").update();
    }

    @Test
    void storesExactDualChunkFormsAndEnforcesTenantAndVectorDimension() {
        insertDocument();
        insertChunk(TENANT_ID, vector(1024));

        Map<String, Object> stored = jdbcClient.sql("""
                        SELECT raw_content, embedding_input, embedding_model_id,
                               embedding_dimensions, embedding_normalized,
                               vector_dims(embedding) AS dimensions
                        FROM knowledge_chunk
                        WHERE id = :id
                        """)
                .param("id", CHUNK_ID)
                .query()
                .singleRow();
        assertThat(stored.get("raw_content")).isEqualTo("Exact source paragraph.");
        assertThat(stored.get("embedding_input")).isEqualTo("""
                Document: Authorization Decline Runbook
                Section: Gateway Failures > Diagnosis
                Type: RUNBOOK
                Applies to: Card authorization

                Exact source paragraph.""");
        assertThat(stored.get("embedding_model_id")).isEqualTo("amazon.titan-embed-text-v2:0");
        assertThat(stored.get("embedding_dimensions")).isEqualTo(1024);
        assertThat(stored.get("embedding_normalized")).isEqualTo(true);
        assertThat(stored.get("dimensions")).isEqualTo(1024);

        assertThatThrownBy(() -> insertChunk(OTHER_TENANT_ID, vector(1024)))
                .isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> insertChunk(TENANT_ID, vector(512)))
                .isInstanceOf(RuntimeException.class);
    }

    private void insertDocument() {
        jdbcClient.sql("""
                        INSERT INTO knowledge_document_version (
                            id, tenant_id, document_id, document_type, title,
                            document_version, incident_family, applies_to,
                            approval_status, approved_by, approved_at, effective_at,
                            source_name, source_content_hash, imported_at
                        ) VALUES (
                            :id, :tenantId, :documentId, 'RUNBOOK',
                            'Authorization Decline Runbook', '1.0.0',
                            'AUTHORIZATION_DECLINE_RATE_SPIKE', 'Card authorization',
                            'APPROVED', :approvedBy,
                            TIMESTAMPTZ '2026-08-20 10:00:00Z',
                            TIMESTAMPTZ '2026-08-21 00:00:00Z',
                            'authorization-decline-runbook.md', :hash,
                            TIMESTAMPTZ '2026-08-28 10:00:00Z'
                        )
                        """)
                .param("id", DOCUMENT_VERSION_ID)
                .param("tenantId", TENANT_ID)
                .param("documentId", DOCUMENT_ID)
                .param("approvedBy", UUID.fromString("7b636625-53d1-46f7-92a9-9c8c27a243d1"))
                .param("hash", "a".repeat(64))
                .update();
    }

    private void insertChunk(UUID tenantId, String embedding) {
        jdbcClient.sql("""
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
                            'Gateway Failures > Diagnosis', 'Exact source paragraph.',
                            :embeddingInput, :rawHash, :embeddingHash,
                            'embedding-input/v1', 'markdown-sections/v1',
                            20, 20, 5, 'amazon.titan-embed-text-v2:0',
                            1024, TRUE, TIMESTAMPTZ '2026-08-28 10:00:00Z',
                            CAST(:embedding AS vector)
                        )
                        """)
                .param("id", CHUNK_ID)
                .param("tenantId", tenantId)
                .param("documentVersionId", DOCUMENT_VERSION_ID)
                .param("embeddingInput", """
                        Document: Authorization Decline Runbook
                        Section: Gateway Failures > Diagnosis
                        Type: RUNBOOK
                        Applies to: Card authorization

                        Exact source paragraph.""")
                .param("rawHash", "b".repeat(64))
                .param("embeddingHash", "c".repeat(64))
                .param("embedding", embedding)
                .update();
    }

    private static String vector(int dimensions) {
        StringBuilder value = new StringBuilder("[");
        for (int index = 0; index < dimensions; index++) {
            if (index > 0) {
                value.append(',');
            }
            value.append(index == 0 ? "1.0" : "0.0");
        }
        return value.append(']').toString();
    }
}
