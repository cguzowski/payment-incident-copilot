import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { EvidenceCollection } from './observed-evidence.models';

@Injectable({ providedIn: 'root' })
export class ObservedEvidenceApiService {
  private readonly http = inject(HttpClient);
  getHistory(investigationId: string): Observable<EvidenceCollection[]> {
    return this.http.get<EvidenceCollection[]>(
      `/api/investigations/${encodeURIComponent(investigationId)}/evidence-collections`,
    );
  }

  collect(investigationId: string): Observable<EvidenceCollection> {
    return this.http.post<EvidenceCollection>(
      `/api/investigations/${encodeURIComponent(investigationId)}/evidence-collections`,
      null,
    );
  }
}
