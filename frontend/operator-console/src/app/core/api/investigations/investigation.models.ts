import { IncidentStatus } from '../../models/incident';

export interface Investigation {
  investigationId: string;
  incidentId: string;
  incidentStatus: IncidentStatus;
  startedBy: string;
  startedAt: string;
}
