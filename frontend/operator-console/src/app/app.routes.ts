import { Routes } from '@angular/router';
import { AlertQueueComponent } from './features/alert-queue/alert-queue.component';
import { IncidentDetailComponent } from './features/incident-detail/incident-detail.component';
import { InvestigationWorkspaceComponent } from './features/investigation-workspace/investigation-workspace.component';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    component: AlertQueueComponent,
    title: 'Incident work queue · Incident Copilot',
  },
  {
    path: 'incidents/:incidentId',
    component: IncidentDetailComponent,
    title: 'Incident detail · Incident Copilot',
  },
  {
    path: 'investigations/:investigationId',
    component: InvestigationWorkspaceComponent,
    title: 'Investigation · Incident Copilot',
  },
];
