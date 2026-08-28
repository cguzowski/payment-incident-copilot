import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { SYNTHETIC_OPERATOR_ID, SYNTHETIC_TENANT_ID } from '../../core/config/synthetic-tenant';
import { Investigation } from './investigation.models';

@Injectable({ providedIn: 'root' })
export class InvestigationApiService {
  private readonly http = inject(HttpClient);
  private readonly params = new HttpParams().set('tenantId', SYNTHETIC_TENANT_ID);

  start(incidentId: string): Observable<Investigation> {
    return this.http.post<Investigation>(
      `/api/incidents/${encodeURIComponent(incidentId)}/investigations`,
      { operatorId: SYNTHETIC_OPERATOR_ID },
      { params: this.params },
    );
  }

  get(investigationId: string): Observable<Investigation> {
    return this.http.get<Investigation>(
      `/api/investigations/${encodeURIComponent(investigationId)}`,
      { params: this.params },
    );
  }
}
