import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { AlertQueueItem } from './alert-queue.models';

@Injectable({ providedIn: 'root' })
export class AlertQueueApiService {
  private readonly http = inject(HttpClient);

  getQueue(): Observable<AlertQueueItem[]> {
    return this.http.get<AlertQueueItem[]>('/api/incidents');
  }
}
