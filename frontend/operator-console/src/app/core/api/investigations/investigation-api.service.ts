import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Investigation } from './investigation.models';

@Injectable({ providedIn: 'root' })
export class InvestigationApiService {
  private readonly http = inject(HttpClient);
  start(incidentId: string): Observable<Investigation> {
    return this.http.post<Investigation>(
      `/api/incidents/${encodeURIComponent(incidentId)}/investigations`,
      null,
    );
  }

  get(investigationId: string): Observable<Investigation> {
    return this.http.get<Investigation>(
      `/api/investigations/${encodeURIComponent(investigationId)}`,
    );
  }
}
