import { IncidentStatus } from '../../../core/models/incident';

export type DecisionOutcome = 'APPROVED' | 'REJECTED';

export interface RecordDecisionCommand {
  outcome: DecisionOutcome;
  reason: string;
}

export interface HumanDecision {
  decisionId: string;
  investigationId: string;
  reportAttemptId: string;
  outcome: DecisionOutcome;
  incidentStatus: Extract<IncidentStatus, 'APPROVED' | 'REJECTED'>;
  reason: string;
  decidedBy: string;
  decidedAt: string;
}
