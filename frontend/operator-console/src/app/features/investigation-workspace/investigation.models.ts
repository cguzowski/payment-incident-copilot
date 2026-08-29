import { IncidentStatus } from '../../core/models/incident';

export interface Investigation {
  investigationId: string;
  incidentId: string;
  incidentStatus: IncidentStatus;
  startedBy: string;
  startedAt: string;
}

export type EvidenceCollectionStatus =
  'STARTED' | 'AVAILABLE' | 'PARTIAL' | 'NOT_FOUND' | 'UNAVAILABLE' | 'TIMED_OUT' | 'MALFORMED';

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

export type KnowledgeRetrievalStatus =
  'STARTED' | 'AVAILABLE' | 'PARTIAL' | 'NO_MATCH' | 'UNAVAILABLE' | 'TIMED_OUT' | 'MALFORMED';

export type KnowledgeDocumentType = 'RUNBOOK' | 'POLICY';

export interface KnowledgeMetadataFilters {
  incidentFamily: string;
  documentTypes: KnowledgeDocumentType[];
  approvalStatus: 'APPROVED';
  effectiveAt: string;
}

export interface KnowledgeRetrievalResult {
  chunkId: string;
  documentVersionId: string;
  documentId: string;
  selectedPosition: number;
  lexicalRank: number | null;
  lexicalPosition: number | null;
  vectorSimilarity: number | null;
  vectorDistance: number | null;
  vectorPosition: number | null;
  fusedPosition: number;
  fusedScore: number;
  documentType: KnowledgeDocumentType;
  documentTitle: string;
  documentVersion: string;
  appliesTo: string;
  sectionPath: string;
  rawContent: string;
  sourceStartLine: number;
  sourceEndLine: number;
  approvalStatus: 'APPROVED';
  approvedBy: string;
  approvedAt: string;
  effectiveAt: string;
}

export interface KnowledgeRetrieval {
  retrievalId: string;
  status: KnowledgeRetrievalStatus;
  requestedAt: string;
  completedAt: string | null;
  queryText: string;
  queryTemplateVersion: string;
  contributingEvidenceIds: string[];
  embeddingModelId: string;
  embeddingDimensions: number;
  metadataFilters: KnowledgeMetadataFilters;
  rankingVersion: string;
  rrfK: number;
  candidateDepth: number;
  minimumLexicalRank: number;
  minimumVectorSimilarity: number;
  statusDetail: string | null;
  results: KnowledgeRetrievalResult[];
}
