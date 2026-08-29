import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { SYNTHETIC_OPERATOR_ID, SYNTHETIC_TENANT_ID } from '../../core/config/synthetic-tenant';
import { EvidenceCollection, Investigation, KnowledgeRetrieval } from './investigation.models';

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

  getEvidenceHistory(investigationId: string): Observable<EvidenceCollection[]> {
    return this.http.get<EvidenceCollection[]>(
      `/api/investigations/${encodeURIComponent(investigationId)}/evidence-collections`,
      { params: this.params },
    );
  }

  collectEvidence(investigationId: string): Observable<EvidenceCollection> {
    return this.http.post<EvidenceCollection>(
      `/api/investigations/${encodeURIComponent(investigationId)}/evidence-collections`,
      null,
      { params: this.params },
    );
  }

  getKnowledgeHistory(investigationId: string): Observable<KnowledgeRetrieval[]> {
    return this.http.get<KnowledgeRetrieval[]>(
      `/api/investigations/${encodeURIComponent(investigationId)}/knowledge-retrievals`,
      { params: this.params },
    );
  }

  retrieveKnowledge(investigationId: string): Observable<KnowledgeRetrieval> {
    return this.http.post<KnowledgeRetrieval>(
      `/api/investigations/${encodeURIComponent(investigationId)}/knowledge-retrievals`,
      null,
      { params: this.params },
    );
  }
}
