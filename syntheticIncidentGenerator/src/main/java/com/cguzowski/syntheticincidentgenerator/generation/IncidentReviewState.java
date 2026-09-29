package com.cguzowski.syntheticincidentgenerator.generation;

import java.util.UUID;

record IncidentReviewState(UUID incidentId, String externalAlertId, String status) {}
