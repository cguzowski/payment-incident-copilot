import { DatePipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  DestroyRef,
  inject,
  input,
  OnInit,
  output,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs';
import { IncidentStatus } from '../../../core/models/incident';
import { ApiRequestError } from '../../../core/http/api-error.interceptor';
import { ReportApiService } from './report-api.service';
import { ReportGenerationAttempt } from './report.models';

type HistoryState = 'loading' | 'success' | 'not-found' | 'error';
type GenerationError = 'conflict' | 'not-found' | 'error' | null;

@Component({
  selector: 'app-report-panel',
  imports: [DatePipe],
  templateUrl: './report-panel.component.html',
  styleUrl: './report-panel.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ReportPanelComponent implements OnInit {
  private readonly api = inject(ReportApiService);
  private readonly destroyRef = inject(DestroyRef);

  readonly investigationId = input.required<string>();
  readonly incidentStatus = input.required<IncidentStatus>();
  readonly reportAvailable = output<void>();
  protected readonly state = signal<HistoryState>('loading');
  protected readonly attempts = signal<ReportGenerationAttempt[]>([]);
  protected readonly generating = signal(false);
  protected readonly generationError = signal<GenerationError>(null);
  protected readonly canGenerate = computed(
    () =>
      this.incidentStatus() === 'INVESTIGATING' &&
      !this.generating() &&
      !this.attempts().some((attempt) => attempt.status === 'AVAILABLE'),
  );

  ngOnInit(): void {
    this.load();
  }

  protected load(): void {
    this.state.set('loading');
    this.api
      .getHistory(this.investigationId())
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (attempts) => {
          this.attempts.set(attempts);
          this.state.set('success');
        },
        error: (error: unknown) => {
          this.state.set(
            error instanceof ApiRequestError && error.status === 404 ? 'not-found' : 'error',
          );
        },
      });
  }

  protected generate(): void {
    if (!this.canGenerate()) {
      return;
    }
    this.generating.set(true);
    this.generationError.set(null);
    this.api
      .generate(this.investigationId())
      .pipe(
        finalize(() => this.generating.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (attempt) => {
          this.attempts.update((attempts) => [attempt, ...attempts]);
          this.state.set('success');
          if (attempt.status === 'AVAILABLE') {
            this.reportAvailable.emit();
          }
        },
        error: (error: unknown) => {
          if (error instanceof ApiRequestError && error.status === 409) {
            this.generationError.set('conflict');
          } else if (error instanceof ApiRequestError && error.status === 404) {
            this.generationError.set('not-found');
          } else {
            this.generationError.set('error');
          }
        },
      });
  }
}
