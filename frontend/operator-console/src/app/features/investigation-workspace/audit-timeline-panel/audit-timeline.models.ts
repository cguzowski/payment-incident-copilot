export type AuditTimelineEventType =
  | 'ALERT_RECEIVED'
  | 'INVESTIGATION_STARTED'
  | 'EVIDENCE_COLLECTION'
  | 'KNOWLEDGE_RETRIEVAL'
  | 'REPORT_GENERATION'
  | 'HUMAN_DECISION';

export type AuditActorKind = 'SYSTEM' | 'OPERATOR' | 'UNATTRIBUTED';

export interface AuditTimelineEvent {
  sourceId: string;
  eventType: AuditTimelineEventType;
  occurredAt: string;
  completedAt: string | null;
  actorKind: AuditActorKind;
  actorId: string | null;
  status: string;
  investigationCorrelationId: string;
  resultingIncidentStatus: string | null;
  relatedSourceId: string | null;
  toolCallId: string | null;
  modelId: string | null;
  promptVersion: string | null;
  schemaVersion: string | null;
  disposition: string | null;
  reason: string | null;
  description: string;
}
