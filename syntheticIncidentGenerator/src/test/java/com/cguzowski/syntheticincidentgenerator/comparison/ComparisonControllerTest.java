package com.cguzowski.syntheticincidentgenerator.comparison;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cguzowski.syntheticincidentgenerator.generation.GeneratorExceptionHandler;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ComparisonControllerTest {
    @Test
    void validIdsReachGatedServiceAndInvalidIdsNeverDo() throws Exception {
        var service = mock(ComparisonService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new ComparisonController(service))
                .setControllerAdvice(new GeneratorExceptionHandler())
                .build();
        UUID incident = UUID.randomUUID(), operator = UUID.randomUUID();
        mvc.perform(post("/api/generations/{id}/comparison", incident).header("X-Synthetic-Operator-Id", operator))
                .andExpect(status().isOk());
        verify(service).compare(incident, operator);
        mvc.perform(post("/api/generations/{id}/comparison", incident)).andExpect(status().isBadRequest());
        mvc.perform(post("/api/generations/not-a-uuid/comparison").header("X-Synthetic-Operator-Id", operator))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/generations/{id}/comparison", incident)
                        .header("X-Synthetic-Operator-Id", operator)
                        .contentType("application/json")
                        .content("{\"answerKey\":{}}"))
                .andExpect(status().isBadRequest());
        verifyNoMoreInteractions(service);
    }
}
