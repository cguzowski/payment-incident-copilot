package com.cguzowski.paymentcopilot.evidence;

import com.cguzowski.paymentcopilot.incident.EvidenceCollectionContext;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class McpServiceErrorEvidenceGateway implements ServiceErrorEvidenceGateway {

    static final String SOURCE_SYSTEM = RecentServiceErrorsPayloadDecoder.SOURCE_SYSTEM;
    static final String SOURCE_TOOL = RecentServiceErrorsPayloadDecoder.SOURCE_TOOL;
    static final String CONTENT_SCHEMA_VERSION = RecentServiceErrorsPayloadDecoder.CONTENT_SCHEMA_VERSION;

    private final RecentServiceErrorsClient client;
    private final RecentServiceErrorsPayloadDecoder decoder;

    McpServiceErrorEvidenceGateway(RecentServiceErrorsClient client, RecentServiceErrorsPayloadDecoder decoder) {
        this.client = client;
        this.decoder = decoder;
    }

    @Override
    public EvidenceSourceResult collect(EvidenceCollectionContext context, UUID toolCallId) {
        try {
            return decoder.decode(client.call(context, toolCallId), context.correlationId(), toolCallId);
        } catch (EvidenceSourceTimedOutException exception) {
            return failure(
                    context.correlationId(),
                    toolCallId,
                    EvidenceCollectionStatus.TIMED_OUT,
                    "Evidence source request timed out.");
        } catch (EvidenceSourceUnavailableException exception) {
            return failure(
                    context.correlationId(),
                    toolCallId,
                    EvidenceCollectionStatus.UNAVAILABLE,
                    "Evidence source is unavailable.");
        } catch (EvidenceSourceMalformedException | InvalidEvidenceSourceResultException exception) {
            return failure(
                    context.correlationId(),
                    toolCallId,
                    EvidenceCollectionStatus.MALFORMED,
                    "Tool result failed validation.");
        }
    }

    private static EvidenceSourceResult failure(
            UUID correlationId, UUID toolCallId, EvidenceCollectionStatus status, String statusDetail) {
        return new EvidenceSourceResult(
                SOURCE_SYSTEM,
                SOURCE_TOOL,
                null,
                correlationId,
                toolCallId,
                status,
                statusDetail,
                CONTENT_SCHEMA_VERSION,
                null);
    }
}
