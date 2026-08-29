package com.cguzowski.paymentcopilot.requestcontext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class SyntheticRequestContextResolverTest {

    private static final String TENANT_ID = "8b860d80-d17f-4e6b-8c48-af35f26a4d61";
    private static final String OPERATOR_ID = "7b636625-53d1-46f7-92a9-9c8c27a243d1";
    private final SyntheticRequestContextResolver resolver = new SyntheticRequestContextResolver();

    @Test
    void resolvesTrimmedSyntheticIdentityHeaders() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(SyntheticRequestContextResolver.TENANT_HEADER, " " + TENANT_ID + " ");
        request.addHeader(SyntheticRequestContextResolver.OPERATOR_HEADER, OPERATOR_ID);

        assertThat(resolver.tenantId(request)).isEqualTo(UUID.fromString(TENANT_ID));
        assertThat(resolver.operatorId(request)).isEqualTo(UUID.fromString(OPERATOR_ID));
    }

    @Test
    void rejectsMissingBlankMalformedAndDuplicateHeaders() {
        MockHttpServletRequest missing = new MockHttpServletRequest();
        assertThatThrownBy(() -> resolver.tenantId(missing))
                .isInstanceOf(InvalidSyntheticRequestContextException.class)
                .hasMessageContaining("is required");

        MockHttpServletRequest blank = new MockHttpServletRequest();
        blank.addHeader(SyntheticRequestContextResolver.TENANT_HEADER, " ");
        assertThatThrownBy(() -> resolver.tenantId(blank)).hasMessageContaining("is required");

        MockHttpServletRequest malformed = new MockHttpServletRequest();
        malformed.addHeader(SyntheticRequestContextResolver.TENANT_HEADER, "not-a-uuid");
        assertThatThrownBy(() -> resolver.tenantId(malformed)).hasMessageContaining("valid UUID");

        MockHttpServletRequest duplicate = new MockHttpServletRequest();
        duplicate.addHeader(SyntheticRequestContextResolver.TENANT_HEADER, TENANT_ID);
        duplicate.addHeader(SyntheticRequestContextResolver.TENANT_HEADER, TENANT_ID);
        assertThatThrownBy(() -> resolver.tenantId(duplicate)).hasMessageContaining("exactly once");
    }

    @Test
    void rejectsLegacyIdentityQueryParametersEvenWhenHeadersArePresent() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(SyntheticRequestContextResolver.TENANT_HEADER, TENANT_ID);
        request.setParameter("tenantId", TENANT_ID);
        assertThatThrownBy(() -> resolver.tenantId(request))
                .isInstanceOf(InvalidSyntheticRequestContextException.class)
                .hasMessageContaining("only through");
    }
}
