import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { ApiRequestError } from '../../core/http/api-error.interceptor';
import { InvestigationApiService } from './investigation-api.service';
import { EvidenceCollection, Investigation, KnowledgeRetrieval } from './investigation.models';

@Component({
  selector: 'app-investigation-workspace',
  imports: [DatePipe, RouterLink],
  styleUrls: [
    '../incident-detail/incident-detail.component.scss',
    './investigation-workspace.component.scss',
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './investigation-workspace.component.html',
})
export class InvestigationWorkspaceComponent {
  private readonly api = inject(InvestigationApiService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly id = inject(ActivatedRoute).snapshot.paramMap.get('investigationId') ?? '';
  protected readonly state = signal<'loading' | 'success' | 'not-found' | 'error'>('loading');
  protected readonly investigation = signal<Investigation | null>(null);
  protected readonly evidenceState = signal<'loading' | 'success' | 'error'>('loading');
  protected readonly evidenceAttempts = signal<EvidenceCollection[]>([]);
  protected readonly collecting = signal(false);
  protected readonly collectionError = signal(false);
  protected readonly knowledgeState = signal<'loading' | 'success' | 'error'>('loading');
  protected readonly knowledgeAttempts = signal<KnowledgeRetrieval[]>([]);
  protected readonly retrievingKnowledge = signal(false);
  protected readonly knowledgeRetrievalError = signal(false);

  constructor() {
    this.load();
  }

  protected load(): void {
    this.state.set('loading');
    this.api
      .get(this.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (item) => {
          this.investigation.set(item);
          this.state.set('success');
          this.loadEvidence();
          this.loadKnowledge();
        },
        error: (error: unknown) =>
          this.state.set(
            error instanceof ApiRequestError && error.status === 404 ? 'not-found' : 'error',
          ),
      });
  }

  protected loadEvidence(): void {
    this.evidenceState.set('loading');
    this.api
      .getEvidenceHistory(this.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (attempts) => {
          this.evidenceAttempts.set(attempts);
          this.evidenceState.set('success');
        },
        error: () => this.evidenceState.set('error'),
      });
  }

  protected collectEvidence(): void {
    if (this.collecting()) {
      return;
    }
    this.collecting.set(true);
    this.collectionError.set(false);
    this.api
      .collectEvidence(this.id)
      .pipe(
        finalize(() => this.collecting.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (attempt) => {
          this.evidenceAttempts.update((attempts) => [attempt, ...attempts]);
          this.evidenceState.set('success');
        },
        error: () => this.collectionError.set(true),
      });
  }

  protected loadKnowledge(): void {
    this.knowledgeState.set('loading');
    this.api
      .getKnowledgeHistory(this.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (attempts) => {
          this.knowledgeAttempts.set(attempts);
          this.knowledgeState.set('success');
        },
        error: () => this.knowledgeState.set('error'),
      });
  }

  protected retrieveKnowledge(): void {
    if (this.retrievingKnowledge()) {
      return;
    }
    this.retrievingKnowledge.set(true);
    this.knowledgeRetrievalError.set(false);
    this.api
      .retrieveKnowledge(this.id)
      .pipe(
        finalize(() => this.retrievingKnowledge.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (attempt) => {
          this.knowledgeAttempts.update((attempts) => [attempt, ...attempts]);
          this.knowledgeState.set('success');
        },
        error: () => this.knowledgeRetrievalError.set(true),
      });
  }
}
