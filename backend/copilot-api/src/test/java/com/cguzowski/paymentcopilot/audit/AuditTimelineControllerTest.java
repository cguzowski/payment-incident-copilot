package com.cguzowski.paymentcopilot.audit;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cguzowski.paymentcopilot.incident.ApiExceptionHandler;
import com.cguzowski.paymentcopilot.requestcontext.SyntheticRequestContextExceptionHandler;
import com.cguzowski.paymentcopilot.requestcontext.SyntheticRequestContextResolver;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AuditTimelineControllerTest {

    private static final UUID TENANT_ID = UUID.fromString("8b860d80-d17f-4e6b-8c48-af35f26a4d61");
    private static final UUID INVESTIGATION_ID = UUID.fromString("a012c9cb-85a6-4d77-9703-3b53377b56c3");
    private static final UUID SOURCE_ID = UUID.fromString("10000000-0000-4000-8000-000000000001");

    private AuditTimelineService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(AuditTimelineService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new AuditTimelineController(service, new SyntheticRequestContextResolver()))
                .setControllerAdvice(
                        new AuditTimelineApiExceptionHandler(),
                        new ApiExceptionHandler(),
                        new SyntheticRequestContextExceptionHandler())
                .setMessageConverters(new JacksonJsonHttpMessageConverter())
                .build();
    }

    @Test
    void returnsTenantScopedSafeTimeline() throws Exception {
        when(service.timeline(TENANT_ID, INVESTIGATION_ID))
                .thenReturn(List.of(new AuditTimelineEvent(
                        SOURCE_ID,
                        AuditTimelineEventType.EVIDENCE_COLLECTION,
                        Instant.parse("2026-08-30T09:02:00Z"),
                        Instant.parse("2026-08-30T09:03:00Z"),
                        AuditActorKind.UNATTRIBUTED,
                        null,
                        "AVAILABLE",
                        UUID.fromString("a5d978b5-34c7-42da-9076-22f8e5169315"),
                        null,
                        null,
                        UUID.fromString("10000000-0000-4000-8000-000000000002"),
                        null,
                        null,
                        "service-errors/v1",
                        null,
                        null,
                        "Synthetic service-error evidence collection.")));

        mockMvc.perform(get("/api/investigations/{investigationId}/timeline", INVESTIGATION_ID)
                        .header("X-Synthetic-Tenant-Id", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sourceId").value(SOURCE_ID.toString()))
                .andExpect(jsonPath("$[0].eventType").value("EVIDENCE_COLLECTION"))
                .andExpect(jsonPath("$[0].actorKind").value("UNATTRIBUTED"))
                .andExpect(jsonPath("$[0].content").doesNotExist())
                .andExpect(jsonPath("$[0].prompt").doesNotExist())
                .andExpect(jsonPath("$[0].statusDetail").doesNotExist());
    }

    @Test
    void rejectsMalformedIdAndHidesCrossTenantLookup() throws Exception {
        mockMvc.perform(get("/api/investigations/not-an-id/timeline").header("X-Synthetic-Tenant-Id", TENANT_ID))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:problem:invalid-audit-timeline-request"));

        when(service.timeline(TENANT_ID, INVESTIGATION_ID)).thenThrow(new AuditTimelineNotFoundException());
        mockMvc.perform(get("/api/investigations/{investigationId}/timeline", INVESTIGATION_ID)
                        .header("X-Synthetic-Tenant-Id", TENANT_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.tenantId").doesNotExist())
                .andExpect(jsonPath("$.investigationId").doesNotExist());
    }
}
