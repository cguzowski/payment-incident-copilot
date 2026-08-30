import { DatePipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  inject,
  input,
  OnInit,
  output,
  signal,
} from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs';
import { ApiRequestError } from '../../../core/http/api-error.interceptor';
import { IncidentStatus } from '../../../core/models/incident';
import { DecisionApiService } from './decision-api.service';
import { DecisionOutcome, HumanDecision } from './decision.models';

type DecisionState = 'loading' | 'success' | 'not-found' | 'error';
type SubmissionError = 'validation' | 'not-found' | 'conflict' | 'error' | null;

@Component({
  selector: 'app-decision-panel',
  imports: [DatePipe, ReactiveFormsModule],
  templateUrl: './decision-panel.component.html',
  styleUrl: './decision-panel.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DecisionPanelComponent implements OnInit {
  private readonly api = inject(DecisionApiService);
  private readonly destroyRef = inject(DestroyRef);

  readonly investigationId = input.required<string>();
  readonly incidentStatus = input.required<IncidentStatus>();
  readonly decisionRecorded = output<HumanDecision>();
  readonly refreshRequested = output<void>();
  protected readonly state = signal<DecisionState>('loading');
  protected readonly decision = signal<HumanDecision | null>(null);
  protected readonly submitting = signal(false);
  protected readonly submissionError = signal<SubmissionError>(null);
  protected readonly form = new FormGroup({
    outcome: new FormControl<DecisionOutcome | null>(null, Validators.required),
    reason: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(1000)],
    }),
  });

  ngOnInit(): void {
    this.load();
  }

  protected load(): void {
    this.state.set('loading');
    this.api
      .getHistory(this.investigationId())
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (decisions) => {
          this.decision.set(decisions[0] ?? null);
          this.state.set('success');
        },
        error: (error: unknown) => {
          this.state.set(
            error instanceof ApiRequestError && error.status === 404 ? 'not-found' : 'error',
          );
        },
      });
  }

  protected submit(): void {
    if (this.submitting() || this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const outcome = this.form.controls.outcome.value;
    const reason = this.form.controls.reason.value.trim();
    if (!outcome || !reason || reason.length > 1000) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    this.submissionError.set(null);
    this.api
      .record(this.investigationId(), { outcome, reason })
      .pipe(
        finalize(() => this.submitting.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (decision) => {
          this.decision.set(decision);
          this.state.set('success');
          this.decisionRecorded.emit(decision);
        },
        error: (error: unknown) => {
          if (error instanceof ApiRequestError && error.status === 400) {
            this.submissionError.set('validation');
          } else if (error instanceof ApiRequestError && error.status === 404) {
            this.submissionError.set('not-found');
          } else if (error instanceof ApiRequestError && error.status === 409) {
            this.submissionError.set('conflict');
            this.refreshRequested.emit();
            this.load();
          } else {
            this.submissionError.set('error');
          }
        },
      });
  }
}
