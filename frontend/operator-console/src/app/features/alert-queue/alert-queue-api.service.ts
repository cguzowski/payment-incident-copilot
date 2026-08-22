import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { AlertQueueItem } from './alert-queue.models';

export const SYNTHETIC_TENANT_ID = '8b860d80-d17f-4e6b-8c48-af35f26a4d61';

@Injectable({ providedIn: 'root' })
export class AlertQueueApiService {
  private readonly http = inject(HttpClient);

  getQueue(): Observable<AlertQueueItem[]> {
    return this.http.get<AlertQueueItem[]>(`/api/tenants/${SYNTHETIC_TENANT_ID}/alert-queue`);
  }
}
