export type ReportGenerationStatus =
  'STARTED' | 'AVAILABLE' | 'UNAVAILABLE' | 'TIMED_OUT' | 'MALFORMED';

export type ReportDisposition = 'PROPOSED' | 'INSUFFICIENT_EVIDENCE';
export type ReportConfidenceLevel = 'LOW' | 'MEDIUM' | 'HIGH';

export interface ReportClaim {
  statement: string;
  evidenceIds: string[];
  knowledgeChunkIds: string[];
}

export interface ReportConfidence {
  level: ReportConfidenceLevel;
  rationale: string;
  evidenceIds: string[];
}

export interface ReportDocument {
  disposition: ReportDisposition;
  summary: ReportClaim;
  observations: ReportClaim[];
  inferences: ReportClaim[];
  probableCause: ReportClaim | null;
  confidence: ReportConfidence;
  recommendation: ReportClaim | null;
  contradictions: ReportClaim[];
  evidenceGaps: { description: string }[];
}

export interface ReportGenerationAttempt {
  attemptId: string;
  investigationId: string;
  status: ReportGenerationStatus;
  requestedAt: string;
  completedAt: string | null;
  modelId: string;
  promptVersion: string;
  schemaVersion: string;
  latestEvidenceId: string;
  applicableEvidenceId: string | null;
  retrievalId: string;
  statusDetail: string | null;
  report: ReportDocument | null;
}
