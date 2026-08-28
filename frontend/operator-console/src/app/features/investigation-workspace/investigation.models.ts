import { IncidentStatus } from '../../core/models/incident';

export interface Investigation {
  investigationId: string;
  incidentId: string;
  incidentStatus: IncidentStatus;
  startedBy: string;
  startedAt: string;
}

export type EvidenceCollectionStatus =
  | 'STARTED'
  | 'AVAILABLE'
  | 'PARTIAL'
  | 'NOT_FOUND'
  | 'UNAVAILABLE'
  | 'TIMED_OUT'
  | 'MALFORMED';

export interface ServiceErrorObservation {
  sourceEventId: string;
  observedAt: string;
  errorCode: string;
  count: number;
}

export interface ServiceErrorEvidenceContent {
  serviceName: string;
  observedFrom: string;
  observedTo: string;
  errors: ServiceErrorObservation[];
}

export interface EvidenceCollection {
  evidenceId: string;
  status: EvidenceCollectionStatus;
  sourceSystem: string;
  sourceTool: string;
  toolCallId: string;
  requestedAt: string;
  retrievedAt: string | null;
  completedAt: string | null;
  contentSchemaVersion: string;
  content: ServiceErrorEvidenceContent | null;
  statusDetail: string | null;
}
