import { DatePipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  computed,
  inject,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { AlertQueueApiService } from './alert-queue-api.service';
import { AlertQueueItem, IncidentSeverity, IncidentStatus } from './alert-queue.models';

type QueueState = 'loading' | 'success' | 'error';
type QueueSort = 'received-desc' | 'received-asc' | 'detected-desc' | 'severity' | 'status';

const severityRank: Record<IncidentSeverity, number> = {
  CRITICAL: 4,
  HIGH: 3,
  MEDIUM: 2,
  LOW: 1,
};
const statusRank: Record<IncidentStatus, number> = { NEW: 1, INVESTIGATING: 2 };

@Component({
  selector: 'app-alert-queue',
  imports: [DatePipe, RouterLink],
  templateUrl: './alert-queue.component.html',
  styleUrl: './alert-queue.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AlertQueueComponent {
  private readonly alertQueueApi = inject(AlertQueueApiService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly state = signal<QueueState>('loading');
  protected readonly incidents = signal<AlertQueueItem[]>([]);
  protected readonly sort = signal<QueueSort>('received-desc');
  protected readonly sortedIncidents = computed(() => {
    const incidents = [...this.incidents()];
    const received = (left: AlertQueueItem, right: AlertQueueItem) =>
      Date.parse(right.receivedAt) - Date.parse(left.receivedAt);
    switch (this.sort()) {
      case 'received-asc':
        return incidents.sort((left, right) => -received(left, right));
      case 'detected-desc':
        return incidents.sort(
          (left, right) => Date.parse(right.detectedAt) - Date.parse(left.detectedAt),
        );
      case 'severity':
        return incidents.sort(
          (left, right) =>
            severityRank[right.severity] - severityRank[left.severity] || received(left, right),
        );
      case 'status':
        return incidents.sort(
          (left, right) =>
            statusRank[left.status] - statusRank[right.status] || received(left, right),
        );
      default:
        return incidents.sort(received);
    }
  });

  constructor() {
    this.loadQueue();
  }

  protected loadQueue(): void {
    this.state.set('loading');
    this.alertQueueApi
      .getQueue()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (incidents) => {
          this.incidents.set(incidents);
          this.state.set('success');
        },
        error: () => {
          this.incidents.set([]);
          this.state.set('error');
        },
      });
  }

  protected onSortChange(event: Event): void {
    const value = (event.target as HTMLSelectElement).value;
    if (['received-desc', 'received-asc', 'detected-desc', 'severity', 'status'].includes(value)) {
      this.sort.set(value as QueueSort);
    }
  }

  protected incidentTypeLabel(): string {
    return 'Authorization decline spike';
  }
}
