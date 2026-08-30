import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { HumanDecision, RecordDecisionCommand } from './decision.models';

@Injectable({ providedIn: 'root' })
export class DecisionApiService {
  private readonly http = inject(HttpClient);

  getHistory(investigationId: string): Observable<HumanDecision[]> {
    return this.http.get<HumanDecision[]>(
      `/api/investigations/${encodeURIComponent(investigationId)}/decisions`,
    );
  }

  record(investigationId: string, command: RecordDecisionCommand): Observable<HumanDecision> {
    return this.http.post<HumanDecision>(
      `/api/investigations/${encodeURIComponent(investigationId)}/decisions`,
      command,
    );
  }
}
