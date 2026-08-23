import { IncidentSeverity, IncidentStatus, IncidentType } from '../../core/models/incident';

export interface IncidentDetail {
  incidentId: string;
  externalAlertId: string;
  incidentType: IncidentType;
  severity: IncidentSeverity;
  status: IncidentStatus;
  title: string;
  description: string;
  detectedAt: string;
  receivedAt: string;
}
