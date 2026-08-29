import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { IncidentDetail } from './incident-detail.models';

@Injectable({ providedIn: 'root' })
export class IncidentDetailApiService {
  private readonly http = inject(HttpClient);

  getDetail(incidentId: string): Observable<IncidentDetail> {
    return this.http.get<IncidentDetail>(`/api/incidents/${encodeURIComponent(incidentId)}`);
  }
}
