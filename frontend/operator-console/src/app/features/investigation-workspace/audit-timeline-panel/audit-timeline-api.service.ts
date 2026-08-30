import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { AuditTimelineEvent } from './audit-timeline.models';

@Injectable({ providedIn: 'root' })
export class AuditTimelineApiService {
  private readonly http = inject(HttpClient);

  getTimeline(investigationId: string): Observable<AuditTimelineEvent[]> {
    return this.http.get<AuditTimelineEvent[]>(
      `/api/investigations/${encodeURIComponent(investigationId)}/timeline`,
    );
  }
}
