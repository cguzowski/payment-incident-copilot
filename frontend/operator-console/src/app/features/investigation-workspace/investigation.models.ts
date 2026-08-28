import { IncidentStatus } from '../../core/models/incident';

export interface Investigation {
  investigationId: string;
  incidentId: string;
  incidentStatus: IncidentStatus;
  startedBy: string;
  startedAt: string;
}
