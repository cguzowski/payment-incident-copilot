import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ReportGenerationAttempt } from './report.models';

@Injectable({ providedIn: 'root' })
export class ReportApiService {
  private readonly http = inject(HttpClient);

  getHistory(investigationId: string): Observable<ReportGenerationAttempt[]> {
    return this.http.get<ReportGenerationAttempt[]>(
      `/api/investigations/${encodeURIComponent(investigationId)}/reports`,
    );
  }

  generate(investigationId: string): Observable<ReportGenerationAttempt> {
    return this.http.post<ReportGenerationAttempt>(
      `/api/investigations/${encodeURIComponent(investigationId)}/reports`,
      null,
    );
  }
}
