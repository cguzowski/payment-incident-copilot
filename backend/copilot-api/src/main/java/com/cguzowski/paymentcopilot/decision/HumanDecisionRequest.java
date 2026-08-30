package com.cguzowski.paymentcopilot.decision;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

record HumanDecisionRequest(
        @NotNull(message = "is required") DecisionOutcome outcome,

        @NotBlank(message = "is required") @Size(max = 1000, message = "must contain at most 1000 characters")
        String reason) {}
