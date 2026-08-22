export type IncidentSeverity = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
export type IncidentStatus = 'NEW';
export type IncidentType = 'AUTHORIZATION_DECLINE_RATE_SPIKE';

export interface AlertQueueItem {
  incidentId: string;
  externalAlertId: string;
  incidentType: IncidentType;
  severity: IncidentSeverity;
  status: IncidentStatus;
  title: string;
  detectedAt: string;
  receivedAt: string;
}
