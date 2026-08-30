import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { InvestigationApiService } from '../../core/api/investigations/investigation-api.service';
import { Investigation } from '../../core/api/investigations/investigation.models';
import { ApiRequestError } from '../../core/http/api-error.interceptor';
import { AuditTimelinePanelComponent } from './audit-timeline-panel/audit-timeline-panel.component';
import { ApprovedKnowledgePanelComponent } from './approved-knowledge-panel/approved-knowledge-panel.component';
import { DecisionPanelComponent } from './decision-panel/decision-panel.component';
import { HumanDecision } from './decision-panel/decision.models';
import { ObservedEvidencePanelComponent } from './observed-evidence-panel/observed-evidence-panel.component';
import { ReportPanelComponent } from './report-panel/report-panel.component';

@Component({
  selector: 'app-investigation-workspace',
  imports: [
    ApprovedKnowledgePanelComponent,
    AuditTimelinePanelComponent,
    DatePipe,
    DecisionPanelComponent,
    ObservedEvidencePanelComponent,
    ReportPanelComponent,
    RouterLink,
  ],
  styleUrl: './investigation-workspace.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './investigation-workspace.component.html',
})
export class InvestigationWorkspaceComponent {
  private readonly api = inject(InvestigationApiService);
  private readonly destroyRef = inject(DestroyRef);
  protected readonly investigationId =
    inject(ActivatedRoute).snapshot.paramMap.get('investigationId') ?? '';
  protected readonly state = signal<'loading' | 'success' | 'not-found' | 'error'>('loading');
  protected readonly investigation = signal<Investigation | null>(null);
  protected readonly hasEvidence = signal(false);
  protected readonly timelineRefresh = signal(0);

  constructor() {
    this.load();
  }

  protected load(): void {
    this.state.set('loading');
    this.api
      .get(this.investigationId)
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

  protected markAwaitingReview(): void {
    this.investigation.update((item) =>
      item ? { ...item, incidentStatus: 'AWAITING_REVIEW' } : item,
    );
  }

  protected handleDecision(decision: HumanDecision): void {
    this.investigation.update((item) =>
      item ? { ...item, incidentStatus: decision.incidentStatus } : item,
    );
    this.timelineRefresh.update((revision) => revision + 1);
  }

  protected refreshAfterDecisionConflict(): void {
    this.api
      .get(this.investigationId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({ next: (item) => this.investigation.set(item) });
    this.timelineRefresh.update((revision) => revision + 1);
  }
}
