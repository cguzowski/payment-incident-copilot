package com.cguzowski.paymentcopilot.incident;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cguzowski.paymentcopilot.requestcontext.SyntheticRequestContextExceptionHandler;
import com.cguzowski.paymentcopilot.requestcontext.SyntheticRequestContextResolver;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

class AlertControllerTest {

    private static final UUID TENANT_ID = UUID.fromString("8b860d80-d17f-4e6b-8c48-af35f26a4d61");
    private static final UUID INCIDENT_ID = UUID.fromString("f4749ecb-49b0-4277-a140-cb69485b082f");

    private AlertIngestionService alertIngestionService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        alertIngestionService = mock(AlertIngestionService.class);
        Validator validator = validator();
        JacksonJsonHttpMessageConverter converter = new JacksonJsonHttpMessageConverter(
                JsonMapper.builder().enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES));
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new AlertController(alertIngestionService, new SyntheticRequestContextResolver()))
                .setControllerAdvice(new ApiExceptionHandler(), new SyntheticRequestContextExceptionHandler())
                .setValidator(validator)
                .setMessageConverters(converter)
                .build();
    }

    @Test
    void validAlertReturnsCreatedIncidentContract() throws Exception {
        Incident incident = new Incident(
                INCIDENT_ID,
                TENANT_ID,
                "alert-auth-decline-001",
                IncidentType.AUTHORIZATION_DECLINE_RATE_SPIKE,
                IncidentSeverity.CRITICAL,
                IncidentStatus.NEW,
                "Authorization decline rate above threshold",
                "Synthetic authorization declines exceeded 25% for five minutes.",
                Instant.parse("2026-08-22T07:14:00Z"),
                Instant.parse("2026-08-22T07:15:00Z"));
        when(alertIngestionService.ingest(any(IngestAlertCommand.class)))
                .thenReturn(new AlertIngestionResult(incident, true));

        mockMvc.perform(post("/api/alerts")
                        .header("X-Synthetic-Tenant-Id", TENANT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "externalAlertId": "alert-auth-decline-001",
                                  "severity": "CRITICAL",
                                  "detectedAt": "2026-08-22T07:14:00Z",
                                  "title": "Authorization decline rate above threshold",
                                  "description": "Synthetic authorization declines exceeded 25% for five minutes."
                                }
                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.incidentId").value(INCIDENT_ID.toString()))
                .andExpect(jsonPath("$.incidentType").value("AUTHORIZATION_DECLINE_RATE_SPIKE"))
                .andExpect(jsonPath("$.status").value("NEW"))
                .andExpect(jsonPath("$.receivedAt").value("2026-08-22T07:15:00Z"));
    }

    @Test
    void missingRequiredFieldsReturnStructuredBadRequest() throws Exception {
        mockMvc.perform(post("/api/alerts")
                        .header("X-Synthetic-Tenant-Id", TENANT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Content-Type", "application/problem+json"))
                .andExpect(jsonPath("$.type").value("urn:problem:invalid-alert"))
                .andExpect(jsonPath("$.title").value("Invalid alert"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("The alert request contains invalid fields."))
                .andExpect(jsonPath("$.errors.length()").value(5))
                .andExpect(jsonPath("$.errors[0].field").value("description"))
                .andExpect(jsonPath("$.errors[1].field").value("detectedAt"))
                .andExpect(jsonPath("$.errors[2].field").value("externalAlertId"))
                .andExpect(jsonPath("$.errors[3].field").value("severity"))
                .andExpect(jsonPath("$.errors[4].field").value("title"));
    }

    @Test
    void unsupportedSeverityReturnsStructuredBadRequest() throws Exception {
        mockMvc.perform(post("/api/alerts")
                        .header("X-Synthetic-Tenant-Id", TENANT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "externalAlertId": "alert-auth-decline-001",
                                  "severity": "URGENT",
                                  "detectedAt": "2026-08-22T07:14:00Z",
                                  "title": "Authorization decline rate above threshold",
                                  "description": "Synthetic authorization declines exceeded 25% for five minutes."
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Content-Type", "application/problem+json"))
                .andExpect(jsonPath("$.type").value("urn:problem:invalid-alert"))
                .andExpect(jsonPath("$.title").value("Invalid alert"))
                .andExpect(jsonPath("$.errors[0].field").value("request"));
    }

    @Test
    void requiresTenantHeaderAndRejectsLegacyTenantBody() throws Exception {
        String body = """
                {
                  "externalAlertId": "alert-auth-decline-001",
                  "severity": "CRITICAL",
                  "detectedAt": "2026-08-22T07:14:00Z",
                  "title": "Authorization decline rate above threshold",
                  "description": "Synthetic authorization declines exceeded 25% for five minutes."
                }
                """;
        mockMvc.perform(post("/api/alerts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:problem:invalid-synthetic-request-context"));

        mockMvc.perform(post("/api/alerts")
                        .header("X-Synthetic-Tenant-Id", TENANT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body.replaceFirst("\\{", "{\"tenantId\":\"" + TENANT_ID + "\",")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:problem:invalid-alert"));
    }

    private static Validator validator() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        return validator;
    }
}
