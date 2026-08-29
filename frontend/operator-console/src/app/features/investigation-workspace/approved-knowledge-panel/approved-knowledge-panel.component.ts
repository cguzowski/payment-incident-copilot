import { DatePipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  inject,
  input,
  OnInit,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs';
import { ApprovedKnowledgeApiService } from './approved-knowledge-api.service';
import { KnowledgeRetrieval } from './approved-knowledge.models';

@Component({
  selector: 'app-approved-knowledge-panel',
  imports: [DatePipe],
  templateUrl: './approved-knowledge-panel.component.html',
  styleUrl: './approved-knowledge-panel.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ApprovedKnowledgePanelComponent implements OnInit {
  private readonly api = inject(ApprovedKnowledgeApiService);
  private readonly destroyRef = inject(DestroyRef);

  readonly investigationId = input.required<string>();
  protected readonly state = signal<'loading' | 'success' | 'error'>('loading');
  protected readonly attempts = signal<KnowledgeRetrieval[]>([]);
  protected readonly retrieving = signal(false);
  protected readonly retrievalError = signal(false);

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
        error: () => this.state.set('error'),
      });
  }

  protected retrieve(): void {
    if (this.retrieving()) {
      return;
    }
    this.retrieving.set(true);
    this.retrievalError.set(false);
    this.api
      .retrieve(this.investigationId())
      .pipe(
        finalize(() => this.retrieving.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (attempt) => {
          this.attempts.update((attempts) => [attempt, ...attempts]);
          this.state.set('success');
        },
        error: () => this.retrievalError.set(true),
      });
  }
}
