import { DatePipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  inject,
  input,
  OnChanges,
  OnInit,
  SimpleChanges,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ApiRequestError } from '../../../core/http/api-error.interceptor';
import { AuditTimelineApiService } from './audit-timeline-api.service';
import { AuditTimelineEvent, AuditTimelineEventType } from './audit-timeline.models';

type TimelineState = 'loading' | 'success' | 'not-found' | 'error';

@Component({
  selector: 'app-audit-timeline-panel',
  imports: [DatePipe],
  templateUrl: './audit-timeline-panel.component.html',
  styleUrl: './audit-timeline-panel.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AuditTimelinePanelComponent implements OnInit, OnChanges {
  private readonly api = inject(AuditTimelineApiService);
  private readonly destroyRef = inject(DestroyRef);

  readonly investigationId = input.required<string>();
  readonly refreshKey = input(0);
  protected readonly state = signal<TimelineState>('loading');
  protected readonly events = signal<AuditTimelineEvent[]>([]);

  ngOnInit(): void {
    this.load();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['refreshKey'] && !changes['refreshKey'].firstChange) {
      this.load();
    }
  }

  protected load(): void {
    this.state.set('loading');
    this.api
      .getTimeline(this.investigationId())
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (events) => {
          this.events.set(events);
          this.state.set('success');
        },
        error: (error: unknown) => {
          this.events.set([]);
          this.state.set(
            error instanceof ApiRequestError && error.status === 404 ? 'not-found' : 'error',
          );
        },
      });
  }

  protected eventLabel(type: AuditTimelineEventType): string {
    const labels: Record<AuditTimelineEventType, string> = {
      ALERT_RECEIVED: 'Alert received',
      INVESTIGATION_STARTED: 'Investigation started',
      EVIDENCE_COLLECTION: 'Evidence collection',
      KNOWLEDGE_RETRIEVAL: 'Approved-knowledge retrieval',
      REPORT_GENERATION: 'Advisory report generation',
      HUMAN_DECISION: 'Human decision',
    };
    return labels[type];
  }
}
