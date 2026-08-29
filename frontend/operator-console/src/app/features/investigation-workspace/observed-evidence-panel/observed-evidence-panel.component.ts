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
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs';
import { ObservedEvidenceApiService } from './observed-evidence-api.service';
import { EvidenceCollection } from './observed-evidence.models';

@Component({
  selector: 'app-observed-evidence-panel',
  imports: [DatePipe],
  templateUrl: './observed-evidence-panel.component.html',
  styleUrl: './observed-evidence-panel.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ObservedEvidencePanelComponent implements OnInit {
  private readonly api = inject(ObservedEvidenceApiService);
  private readonly destroyRef = inject(DestroyRef);

  readonly investigationId = input.required<string>();
  readonly historyPresence = output<boolean>();
  protected readonly state = signal<'loading' | 'success' | 'error'>('loading');
  protected readonly attempts = signal<EvidenceCollection[]>([]);
  protected readonly collecting = signal(false);
  protected readonly collectionError = signal(false);

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
          this.historyPresence.emit(attempts.length > 0);
        },
        error: () => this.state.set('error'),
      });
  }

  protected collect(): void {
    if (this.collecting()) {
      return;
    }
    this.collecting.set(true);
    this.collectionError.set(false);
    this.api
      .collect(this.investigationId())
      .pipe(
        finalize(() => this.collecting.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (attempt) => {
          this.attempts.update((attempts) => [attempt, ...attempts]);
          this.state.set('success');
          this.historyPresence.emit(true);
        },
        error: () => this.collectionError.set(true),
      });
  }
}
