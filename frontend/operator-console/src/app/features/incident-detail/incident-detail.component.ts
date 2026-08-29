import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { InvestigationApiService } from '../../core/api/investigations/investigation-api.service';
import { ApiRequestError } from '../../core/http/api-error.interceptor';
import { IncidentDetailApiService } from './incident-detail-api.service';
import { IncidentDetail } from './incident-detail.models';

type DetailState = 'loading' | 'success' | 'not-found' | 'error';

@Component({
  selector: 'app-incident-detail',
  imports: [DatePipe, RouterLink],
  templateUrl: './incident-detail.component.html',
  styleUrl: './incident-detail.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class IncidentDetailComponent {
  private readonly incidentDetailApi = inject(IncidentDetailApiService);
  private readonly investigationApi = inject(InvestigationApiService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private readonly incidentId = inject(ActivatedRoute).snapshot.paramMap.get('incidentId') ?? '';

  protected readonly state = signal<DetailState>('loading');
  protected readonly incident = signal<IncidentDetail | null>(null);
  protected readonly starting = signal(false);
  protected readonly startError = signal(false);

  constructor() {
    this.loadDetail();
  }

  protected loadDetail(): void {
    this.state.set('loading');
    this.incident.set(null);
    this.incidentDetailApi
      .getDetail(this.incidentId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (incident) => {
          this.incident.set(incident);
          this.state.set('success');
        },
        error: (error: unknown) => {
          this.state.set(
            error instanceof ApiRequestError && error.status === 404 ? 'not-found' : 'error',
          );
        },
      });
  }

  protected incidentTypeLabel(): string {
    return 'Authorization decline spike';
  }

  protected startInvestigation(): void {
    if (this.starting()) return;
    this.starting.set(true);
    this.startError.set(false);
    this.investigationApi
      .start(this.incidentId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: ({ investigationId }) =>
          void this.router.navigate(['/investigations', investigationId]),
        error: () => {
          this.starting.set(false);
          this.startError.set(true);
        },
      });
  }
}
