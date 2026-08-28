import { IncidentSeverity, IncidentStatus, IncidentType } from '../../core/models/incident';

export type { IncidentSeverity, IncidentStatus, IncidentType } from '../../core/models/incident';

export interface AlertQueueItem {
  incidentId: string;
  externalAlertId: string;
  incidentType: IncidentType;
  severity: IncidentSeverity;
  status: IncidentStatus;
  title: string;
  detectedAt: string;
  receivedAt: string;
  activeInvestigationId: string | null;
}
