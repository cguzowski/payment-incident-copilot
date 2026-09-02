export type KnowledgeRetrievalStatus =
  'STARTED' | 'AVAILABLE' | 'PARTIAL' | 'NO_MATCH' | 'UNAVAILABLE' | 'TIMED_OUT' | 'MALFORMED';

export type KnowledgeDocumentType = 'RUNBOOK' | 'POLICY';
export type KnowledgeSourceFormat = 'MARKDOWN' | 'PDF';

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
  sourceName: string;
  sourceFormat: KnowledgeSourceFormat;
  pdfSha256: string | null;
  sourceStartLine: number | null;
  sourceEndLine: number | null;
  sourceStartPage: number | null;
  sourceEndPage: number | null;
  sourceStartBlock: number | null;
  sourceEndBlock: number | null;
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
