import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { KnowledgeRetrieval } from './approved-knowledge.models';

@Injectable({ providedIn: 'root' })
export class ApprovedKnowledgeApiService {
  private readonly http = inject(HttpClient);
  getHistory(investigationId: string): Observable<KnowledgeRetrieval[]> {
    return this.http.get<KnowledgeRetrieval[]>(
      `/api/investigations/${encodeURIComponent(investigationId)}/knowledge-retrievals`,
    );
  }

  retrieve(investigationId: string): Observable<KnowledgeRetrieval> {
    return this.http.post<KnowledgeRetrieval>(
      `/api/investigations/${encodeURIComponent(investigationId)}/knowledge-retrievals`,
      null,
    );
  }
}
