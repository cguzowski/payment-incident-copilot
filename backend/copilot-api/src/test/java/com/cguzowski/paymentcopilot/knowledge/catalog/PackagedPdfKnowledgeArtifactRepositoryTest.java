package com.cguzowski.paymentcopilot.knowledge.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

class PackagedPdfKnowledgeArtifactRepositoryTest {

    @Test
    void resolvesFrozenPaymentLibraryPdfWithExactBytes() throws Exception {
        var repository = new PackagedPdfKnowledgeArtifactRepository(new PathMatchingResourcePatternResolver());
        var artifact = repository
                .findBySha256("f4cc3d2f22bcf130dd399bf8ec08763428ddc72bc63d9438d69eae5736f46359")
                .orElseThrow();
        assertThat(artifact.content())
                .isEqualTo(java.nio.file.Files.readAllBytes(PaymentPdfCatalogTest.ROOT.resolve(
                        "pdfs/rb-101-authorization-rejection-establish-the-source.pdf")));
    }

    @Test
    void resolvesOnlyPackagedManifestPdfByItsExactHash() {
        PackagedPdfKnowledgeArtifactRepository repository =
                new PackagedPdfKnowledgeArtifactRepository(new PathMatchingResourcePatternResolver());

        PdfKnowledgeArtifact artifact = repository
                .findBySha256("804ece64d3bbbcb98d99ee0537a7975f2b038ffae4e51e1226977a0fd2b3fa79")
                .orElseThrow();

        assertThat(artifact.fileName()).isEqualTo("rb-001-authorization-decline-incident-triage-v2.1.0.pdf");
        assertThat(artifact.content()).startsWith("%PDF".getBytes());
        assertThat(repository.findBySha256("0".repeat(64))).isEmpty();
    }

    @Test
    void resolvesAnImmutablePdfFromAPreviousKnowledgeCorpusVersion() {
        PackagedPdfKnowledgeArtifactRepository repository =
                new PackagedPdfKnowledgeArtifactRepository(new PathMatchingResourcePatternResolver());

        PdfKnowledgeArtifact artifact = repository
                .findBySha256("4132a6f7a627247c40c5caf2d57b7ca8d200840ea5dbabb9676a8d272168d940")
                .orElseThrow();

        assertThat(artifact.fileName()).isEqualTo("rb-001-authorization-decline-incident-triage-v2.0.0.pdf");
        assertThat(artifact.content()).startsWith("%PDF".getBytes());
    }
}
