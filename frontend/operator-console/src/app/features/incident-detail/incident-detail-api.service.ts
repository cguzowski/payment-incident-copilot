import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { SYNTHETIC_TENANT_ID } from '../../core/config/synthetic-tenant';
import { IncidentDetail } from './incident-detail.models';

@Injectable({ providedIn: 'root' })
export class IncidentDetailApiService {
  private readonly http = inject(HttpClient);

  getDetail(incidentId: string): Observable<IncidentDetail> {
    const params = new HttpParams().set('tenantId', SYNTHETIC_TENANT_ID);
    return this.http.get<IncidentDetail>(`/api/incidents/${encodeURIComponent(incidentId)}`, {
      params,
    });
  }
}
