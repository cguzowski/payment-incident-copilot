package com.cguzowski.paymentcopilot.requestcontext;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public final class SyntheticRequestContextResolver {

    public static final String TENANT_HEADER = "X-Synthetic-Tenant-Id";
    public static final String OPERATOR_HEADER = "X-Synthetic-Operator-Id";

    public UUID tenantId(HttpServletRequest request) {
        rejectLegacyIdentityParameters(request);
        return requiredUuidHeader(request, TENANT_HEADER, "tenantId");
    }

    public UUID operatorId(HttpServletRequest request) {
        rejectLegacyIdentityParameters(request);
        return requiredUuidHeader(request, OPERATOR_HEADER, "operatorId");
    }

    private static UUID requiredUuidHeader(HttpServletRequest request, String headerName, String field) {
        List<String> values = Collections.list(request.getHeaders(headerName));
        if (values.isEmpty() || values.getFirst().isBlank()) {
            throw new InvalidSyntheticRequestContextException(field, headerName + " is required");
        }
        if (values.size() != 1) {
            throw new InvalidSyntheticRequestContextException(field, headerName + " must appear exactly once");
        }
        try {
            return UUID.fromString(values.getFirst().trim());
        } catch (IllegalArgumentException exception) {
            throw new InvalidSyntheticRequestContextException(field, headerName + " must be a valid UUID");
        }
    }

    private static void rejectLegacyIdentityParameters(HttpServletRequest request) {
        if (request.getParameterMap().containsKey("tenantId")) {
            throw new InvalidSyntheticRequestContextException(
                    "tenantId", "must be supplied only through " + TENANT_HEADER);
        }
        if (request.getParameterMap().containsKey("operatorId")) {
            throw new InvalidSyntheticRequestContextException(
                    "operatorId", "must be supplied only through " + OPERATOR_HEADER);
        }
    }
}
