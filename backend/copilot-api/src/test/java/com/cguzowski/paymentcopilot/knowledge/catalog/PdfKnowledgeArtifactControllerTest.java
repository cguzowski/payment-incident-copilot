package com.cguzowski.paymentcopilot.knowledge.catalog;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class PdfKnowledgeArtifactControllerTest {

    private static final String HASH = "d".repeat(64);
    private PdfKnowledgeArtifactRepository repository;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        repository = mock(PdfKnowledgeArtifactRepository.class);
        PdfKnowledgeArtifactService service = new PdfKnowledgeArtifactService(repository);
        mockMvc = MockMvcBuilders.standaloneSetup(new PdfKnowledgeArtifactController(service))
                .setControllerAdvice(new PdfKnowledgeArtifactExceptionHandler())
                .build();
    }

    @Test
    void returnsExactInlinePdfForContentHash() throws Exception {
        when(repository.findBySha256(HASH))
                .thenReturn(Optional.of(new PdfKnowledgeArtifact(HASH, "synthetic.pdf", "%PDF-test".getBytes())));

        mockMvc.perform(get("/api/knowledge/pdf-artifacts/{sha256}", HASH))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(header().string("Content-Disposition", "inline; filename=\"synthetic.pdf\""))
                .andExpect(header().string("Cache-Control", "max-age=31536000, public, immutable"))
                .andExpect(content().bytes("%PDF-test".getBytes()));
    }

    @Test
    void rejectsMalformedAndUnknownHashesWithoutServingAnotherArtifact() throws Exception {
        mockMvc.perform(get("/api/knowledge/pdf-artifacts/not-a-hash")).andExpect(status().isBadRequest());
        when(repository.findBySha256(HASH)).thenReturn(Optional.empty());
        mockMvc.perform(get("/api/knowledge/pdf-artifacts/{sha256}", HASH)).andExpect(status().isNotFound());
    }
}
