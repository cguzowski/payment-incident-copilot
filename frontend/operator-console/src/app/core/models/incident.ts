export type IncidentSeverity = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
export type IncidentStatus = 'NEW' | 'INVESTIGATING' | 'AWAITING_REVIEW' | 'APPROVED' | 'REJECTED';
export type IncidentType =
  | 'AUTHORIZATION_DECLINE_RATE_SPIKE'
  | 'AUTHORIZATION_TIMEOUT_SPIKE'
  | 'CAPTURE_FAILURE_SPIKE'
  | 'REFUND_FAILURE_SPIKE'
  | 'SETTLEMENT_DELAY'
  | 'WEBHOOK_DELIVERY_FAILURE'
  | 'RECONCILIATION_MISMATCH';

export const incidentTypeLabels: Record<IncidentType, string> = {
  AUTHORIZATION_DECLINE_RATE_SPIKE: 'Authorization decline spike',
  AUTHORIZATION_TIMEOUT_SPIKE: 'Authorization timeout spike',
  CAPTURE_FAILURE_SPIKE: 'Capture failure spike',
  REFUND_FAILURE_SPIKE: 'Refund failure spike',
  SETTLEMENT_DELAY: 'Settlement delay',
  WEBHOOK_DELIVERY_FAILURE: 'Webhook delivery failure',
  RECONCILIATION_MISMATCH: 'Reconciliation mismatch',
};
