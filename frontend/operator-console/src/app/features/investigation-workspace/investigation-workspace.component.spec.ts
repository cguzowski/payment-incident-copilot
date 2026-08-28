import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { Observable, Subject, of, throwError } from 'rxjs';
import { ApiRequestError } from '../../core/http/api-error.interceptor';
import {
  EvidenceCollection,
  EvidenceCollectionStatus,
  Investigation,
} from './investigation.models';
import { InvestigationApiService } from './investigation-api.service';
import { InvestigationWorkspaceComponent } from './investigation-workspace.component';

describe('InvestigationWorkspaceComponent', () => {
  let response: Observable<Investigation>;
  let historyResponse: Observable<EvidenceCollection[]>;
  let collectionResponse: Observable<EvidenceCollection>;
  let api: {
    get: ReturnType<typeof vi.fn>;
    getEvidenceHistory: ReturnType<typeof vi.fn>;
    collectEvidence: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    response = new Subject<Investigation>().asObservable();
    historyResponse = of([]);
    collectionResponse = of(evidence('AVAILABLE'));
    api = {
      get: vi.fn(() => response),
      getEvidenceHistory: vi.fn(() => historyResponse),
      collectEvidence: vi.fn(() => collectionResponse),
    };
    await TestBed.configureTestingModule({
      imports: [InvestigationWorkspaceComponent],
      providers: [
        { provide: InvestigationApiService, useValue: api },
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: convertToParamMap({
                investigationId: 'a012c9cb-85a6-4d77-9703-3b53377b56c3',
              }),
            },
          },
        },
      ],
    }).compileComponents();
  });

  it('showsLoadingState', () => {
    const fixture = TestBed.createComponent(InvestigationWorkspaceComponent);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[role="status"]').textContent).toContain('Loading');
  });

  it('rendersInvestigationAndLinksBackToIncidentAndWorkQueue', () => {
    response = of(investigation());
    const fixture = TestBed.createComponent(InvestigationWorkspaceComponent);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Evidence collection has not started');
    expect(fixture.nativeElement.textContent).toContain('INVESTIGATING');
    expect(
      fixture.nativeElement.querySelector('[data-testid="incident-link"]').getAttribute('href'),
    ).toBe('/incidents/f4749ecb-49b0-4277-a140-cb69485b082f');
    expect(
      fixture.nativeElement.querySelector('[data-testid="queue-link"]').getAttribute('href'),
    ).toBe('/');
    expect(fixture.nativeElement.querySelector('[data-testid="queue-link"]').classList).toContain(
      'action-link',
    );
  });

  it('usesCompactWorkspaceStatusAndIncidentActionStyles', () => {
    response = of(investigation());
    const fixture = TestBed.createComponent(InvestigationWorkspaceComponent);
    fixture.detectChanges();

    const status = fixture.nativeElement.querySelector('.status') as HTMLElement;
    const incidentLink = fixture.nativeElement.querySelector(
      '[data-testid="incident-link"]',
    ) as HTMLAnchorElement;

    expect(status.classList).toContain('status--compact');
    expect(incidentLink.classList).toContain('action-link');
    expect(incidentLink.closest('[data-testid="workspace-actions"]')).not.toBeNull();
  });

  it('loadsEvidenceHistoryIndependentlyAndShowsNotCollectedState', () => {
    response = of(investigation());
    const history = new Subject<EvidenceCollection[]>();
    historyResponse = history.asObservable();
    const fixture = TestBed.createComponent(InvestigationWorkspaceComponent);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-testid="evidence-loading"]')).not.toBeNull();

    history.next([]);
    history.complete();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Evidence collection has not started');
  });

  it('rendersAvailableEvidenceWithProvenanceAndNoInference', () => {
    response = of(investigation());
    historyResponse = of([evidence('AVAILABLE')]);
    const fixture = TestBed.createComponent(InvestigationWorkspaceComponent);
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent;
    expect(text).toContain('Observed evidence');
    expect(text).toContain('payment-authorization-service');
    expect(text).toContain('UPSTREAM_TIMEOUT');
    expect(text).toContain('synthetic-observability');
    expect(text).toContain('getRecentServiceErrors');
    expect(text).toContain('21fdc56b-267a-4cb5-81b9-50f092e0ef35');
    expect(text).toContain('Observed synthetic evidence');
    expect(text).toContain('No AI inference or recommendation has been generated');
  });

  it('rendersAvailableEmptyEvidenceAsSuccessfulObservation', () => {
    response = of(investigation());
    const empty = evidence('AVAILABLE');
    empty.content = { ...empty.content!, errors: [] };
    historyResponse = of([empty]);
    const fixture = TestBed.createComponent(InvestigationWorkspaceComponent);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('No service errors were observed');
    expect(fixture.nativeElement.textContent).not.toContain('Evidence source unavailable');
  });

  it('distinguishesPartialUnavailableTimedOutMalformedAndInterruptedEvidence', () => {
    response = of(investigation());
    const cases: [EvidenceCollectionStatus, string][] = [
      ['PARTIAL', 'Only part of the synthetic observation window was available.'],
      ['NOT_FOUND', 'No matching synthetic evidence was found.'],
      ['UNAVAILABLE', 'Evidence source unavailable.'],
      ['TIMED_OUT', 'Evidence source request timed out.'],
      ['MALFORMED', 'Tool result failed validation.'],
      ['STARTED', 'Collection did not reach a terminal status.'],
    ];

    for (const [status, detail] of cases) {
      historyResponse = of([evidence(status, detail)]);
      const fixture = TestBed.createComponent(InvestigationWorkspaceComponent);
      fixture.detectChanges();
      expect(fixture.nativeElement.textContent).toContain(status);
      expect(fixture.nativeElement.textContent).toContain(detail);
      fixture.destroy();
    }
  });

  it('disablesCollectionWhilePendingAndAppendsRetryWithoutHidingHistory', () => {
    response = of(investigation());
    historyResponse = of([evidence('AVAILABLE')]);
    const pending = new Subject<EvidenceCollection>();
    collectionResponse = pending.asObservable();
    const fixture = TestBed.createComponent(InvestigationWorkspaceComponent);
    fixture.detectChanges();

    const button = fixture.nativeElement.querySelector(
      '[data-testid="collect-evidence"]',
    ) as HTMLButtonElement;
    button.click();
    fixture.detectChanges();
    expect(button.disabled).toBe(true);
    expect(fixture.nativeElement.textContent).toContain('a8bab9d4-dccc-4e70-acfe-174ac63a3b12');

    pending.next({
      ...evidence('UNAVAILABLE', 'Evidence source unavailable.'),
      evidenceId: '67d4709a-a5dc-4356-8351-af86878c2e2d',
    });
    pending.complete();
    fixture.detectChanges();
    expect(button.disabled).toBe(false);
    const attemptText = Array.from(
      fixture.nativeElement.querySelectorAll('[data-testid="evidence-attempt"]'),
    ).map((element) => (element as HTMLElement).textContent);
    expect(attemptText[0]).toContain('67d4709a-a5dc-4356-8351-af86878c2e2d');
    expect(attemptText[1]).toContain('a8bab9d4-dccc-4e70-acfe-174ac63a3b12');
  });

  it('showsRetryableEvidenceApiFailureWithoutReplacingInvestigation', () => {
    response = of(investigation());
    historyResponse = throwError(() => new ApiRequestError('unavailable', 503));
    const fixture = TestBed.createComponent(InvestigationWorkspaceComponent);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('We couldn’t load evidence history');
    expect(fixture.nativeElement.textContent).toContain('Investigation ID');

    historyResponse = of([]);
    fixture.nativeElement.querySelector('[data-testid="retry-evidence"]').click();
    fixture.detectChanges();
    expect(api.getEvidenceHistory).toHaveBeenCalledTimes(2);
    expect(fixture.nativeElement.textContent).toContain('Evidence collection has not started');
  });

  it('showsNotFoundAndRetryableErrorStates', () => {
    response = throwError(() => new ApiRequestError('not found', 404));
    const notFound = TestBed.createComponent(InvestigationWorkspaceComponent);
    notFound.detectChanges();
    expect(notFound.nativeElement.textContent).toContain('Investigation not found');

    response = throwError(() => new ApiRequestError('unavailable', 503));
    const error = TestBed.createComponent(InvestigationWorkspaceComponent);
    error.detectChanges();
    const retry = error.nativeElement.querySelector('[data-testid="retry"]') as HTMLButtonElement;
    expect(retry.classList).toContain('button');
    expect(retry.classList).toContain('button--primary');
  });

  function investigation(): Investigation {
    return {
      investigationId: 'a012c9cb-85a6-4d77-9703-3b53377b56c3',
      incidentId: 'f4749ecb-49b0-4277-a140-cb69485b082f',
      incidentStatus: 'INVESTIGATING',
      startedBy: '7b636625-53d1-46f7-92a9-9c8c27a243d1',
      startedAt: '2026-08-27T18:30:00Z',
    };
  }

  function evidence(
    status: EvidenceCollectionStatus,
    statusDetail: string | null = null,
  ): EvidenceCollection {
    const hasContent = status === 'AVAILABLE' || status === 'PARTIAL';
    return {
      evidenceId: 'a8bab9d4-dccc-4e70-acfe-174ac63a3b12',
      status,
      sourceSystem: 'synthetic-observability',
      sourceTool: 'getRecentServiceErrors',
      toolCallId: '21fdc56b-267a-4cb5-81b9-50f092e0ef35',
      requestedAt: '2026-08-28T10:00:00Z',
      retrievedAt: status === 'STARTED' ? null : '2026-08-28T10:00:01Z',
      completedAt: status === 'STARTED' ? null : '2026-08-28T10:00:02Z',
      contentSchemaVersion: 'service-errors/v1',
      content: hasContent
        ? {
            serviceName: 'payment-authorization-service',
            observedFrom: '2026-08-28T09:55:00Z',
            observedTo: '2026-08-28T10:00:00Z',
            errors: [
              {
                sourceEventId: 'service-error-001',
                observedAt: '2026-08-28T09:58:00Z',
                errorCode: 'UPSTREAM_TIMEOUT',
                count: 14,
              },
            ],
          }
        : null,
      statusDetail,
    };
  }
});
