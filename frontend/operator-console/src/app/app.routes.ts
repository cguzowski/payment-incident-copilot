import { Routes } from '@angular/router';
import { AlertQueueComponent } from './features/alert-queue/alert-queue.component';
import { IncidentDetailComponent } from './features/incident-detail/incident-detail.component';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    component: AlertQueueComponent,
    title: 'Alert queue · Incident Copilot',
  },
  {
    path: 'incidents/:incidentId',
    component: IncidentDetailComponent,
    title: 'Incident detail · Incident Copilot',
  },
];
