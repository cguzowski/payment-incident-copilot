import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ApiRequestError } from '../../core/http/api-error.interceptor';
import { InvestigationApiService } from './investigation-api.service';
import { Investigation } from './investigation.models';

const WORKSPACE_TEMPLATE = `<section class="detail" aria-labelledby="detail-title">
  <a class="back-link action-link" data-testid="queue-link" routerLink="/">← Back to work queue</a>
  @if (state() === 'loading') { <p class="state-card" role="status"><strong>Loading investigation…</strong></p> }
  @else if (state() === 'not-found') { <p class="state-card"><strong>Investigation not found</strong></p> }
  @else if (state() === 'error') { <p class="state-card" role="alert"><strong>We couldn’t load this investigation.</strong><button class="button button--primary" type="button" data-testid="retry" (click)="load()">Try again</button></p> }
  @else if (investigation(); as item) { <article class="incident-card"><p class="eyebrow">Investigation workspace</p><h1 id="detail-title">Investigation</h1><p class="description">Evidence collection has not started.</p>
    <dl class="facts"><div><dt>Investigation ID</dt><dd><code>{{ item.investigationId }}</code></dd></div><div><dt>Status</dt><dd><span class="status status--compact">{{ item.incidentStatus }}</span></dd></div><div><dt>Started by</dt><dd><code>{{ item.startedBy }}</code></dd></div><div><dt>Started</dt><dd>{{ item.startedAt | date: 'MMM d, y, HH:mm:ss' : 'UTC' }} UTC</dd></div></dl>
    <div class="workspace-actions" data-testid="workspace-actions"><a class="action-link" data-testid="incident-link" [routerLink]="['/incidents', item.incidentId]">Open incident detail</a></div></article> }
</section>`;

@Component({
  selector: 'app-investigation-workspace',
  imports: [DatePipe, RouterLink],
  styleUrl: '../incident-detail/incident-detail.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: WORKSPACE_TEMPLATE,
})
export class InvestigationWorkspaceComponent {
  private readonly api = inject(InvestigationApiService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly id = inject(ActivatedRoute).snapshot.paramMap.get('investigationId') ?? '';
  protected readonly state = signal<'loading' | 'success' | 'not-found' | 'error'>('loading');
  protected readonly investigation = signal<Investigation | null>(null);

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
        },
        error: (error: unknown) =>
          this.state.set(
            error instanceof ApiRequestError && error.status === 404 ? 'not-found' : 'error',
          ),
      });
  }
}
