import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { Observable, Subject, of, throwError } from 'rxjs';
import { ApiRequestError } from '../../core/http/api-error.interceptor';
import {
  EvidenceCollection,
  EvidenceCollectionStatus,
  Investigation,
  KnowledgeRetrieval,
  KnowledgeRetrievalStatus,
} from './investigation.models';
import { InvestigationApiService } from './investigation-api.service';
import { InvestigationWorkspaceComponent } from './investigation-workspace.component';

describe('InvestigationWorkspaceComponent', () => {
  let response: Observable<Investigation>;
  let historyResponse: Observable<EvidenceCollection[]>;
  let collectionResponse: Observable<EvidenceCollection>;
  let knowledgeHistoryResponse: Observable<KnowledgeRetrieval[]>;
  let retrievalResponse: Observable<KnowledgeRetrieval>;
  let api: {
    get: ReturnType<typeof vi.fn>;
    getEvidenceHistory: ReturnType<typeof vi.fn>;
    collectEvidence: ReturnType<typeof vi.fn>;
    getKnowledgeHistory: ReturnType<typeof vi.fn>;
    retrieveKnowledge: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    response = new Subject<Investigation>().asObservable();
    historyResponse = of([]);
    collectionResponse = of(evidence('AVAILABLE'));
    knowledgeHistoryResponse = of([]);
    retrievalResponse = of(knowledge('AVAILABLE'));
    api = {
      get: vi.fn(() => response),
      getEvidenceHistory: vi.fn(() => historyResponse),
      collectEvidence: vi.fn(() => collectionResponse),
      getKnowledgeHistory: vi.fn(() => knowledgeHistoryResponse),
      retrieveKnowledge: vi.fn(() => retrievalResponse),
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

  it('loadsKnowledgeHistoryIndependentlyAndShowsNotRetrievedState', () => {
    response = of(investigation());
    const history = new Subject<KnowledgeRetrieval[]>();
    knowledgeHistoryResponse = history.asObservable();
    const fixture = TestBed.createComponent(InvestigationWorkspaceComponent);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-testid="knowledge-loading"]')).not.toBeNull();

    history.next([]);
    history.complete();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain(
      'Approved knowledge has not been retrieved',
    );
  });

  it('rendersMatchedRunbookAndPolicyChunksWithRawContentAndProvenance', () => {
    response = of(investigation());
    knowledgeHistoryResponse = of([knowledge('AVAILABLE')]);
    const fixture = TestBed.createComponent(InvestigationWorkspaceComponent);
    fixture.detectChanges();

    const panel = fixture.nativeElement.querySelector(
      '[data-testid="knowledge-panel"]',
    ) as HTMLElement;
    expect(panel.textContent).toContain('Approved knowledge');
    expect(panel.textContent).toContain('Authorization Decline Runbook');
    expect(panel.textContent).toContain('Payment Incident Response Policy');
    expect(panel.textContent).toContain('Gateway Failures > Diagnosis');
    expect(panel.textContent).toContain('Inspect GATEWAY_TIMEOUT observations.');
    expect(panel.textContent).toContain('21111111-1111-4111-8111-111111111111');
    expect(panel.textContent).toContain('Synthetic approved source material');
    expect(panel.textContent).toContain('not an AI conclusion or executable instruction');
    expect(panel.textContent).not.toContain('Document: Authorization Decline Runbook');
  });

  it('distinguishesNoMatchDegradedUnavailableMalformedAndInterruptedRetrieval', () => {
    response = of(investigation());
    const cases: [KnowledgeRetrievalStatus, string][] = [
      ['PARTIAL', 'Query embedding was unavailable; lexical retrieval was used.'],
      ['NO_MATCH', 'No eligible approved knowledge matched this investigation.'],
      ['UNAVAILABLE', 'Query embedding was unavailable and lexical retrieval returned no match.'],
      ['TIMED_OUT', 'Query embedding timed out and lexical retrieval returned no match.'],
      ['MALFORMED', 'Query embedding output was malformed.'],
      ['STARTED', 'Retrieval did not reach a terminal status.'],
    ];

    for (const [status, detail] of cases) {
      knowledgeHistoryResponse = of([knowledge(status, status === 'NO_MATCH' ? null : detail, [])]);
      const fixture = TestBed.createComponent(InvestigationWorkspaceComponent);
      fixture.detectChanges();
      const panel = fixture.nativeElement.querySelector('[data-testid="knowledge-panel"]');
      expect(panel.textContent).toContain(status);
      expect(panel.textContent).toContain(detail);
      fixture.destroy();
    }
  });

  it('disablesRetrievalWhilePendingAndAppendsRetryWithoutHidingHistory', () => {
    response = of(investigation());
    knowledgeHistoryResponse = of([knowledge('AVAILABLE')]);
    const pending = new Subject<KnowledgeRetrieval>();
    retrievalResponse = pending.asObservable();
    const fixture = TestBed.createComponent(InvestigationWorkspaceComponent);
    fixture.detectChanges();

    const button = fixture.nativeElement.querySelector(
      '[data-testid="retrieve-knowledge"]',
    ) as HTMLButtonElement;
    button.click();
    fixture.detectChanges();
    expect(button.disabled).toBe(true);

    pending.next({
      ...knowledge('NO_MATCH', null, []),
      retrievalId: 'd84b2fb0-3436-4c61-afdf-a673535fc6cc',
    });
    pending.complete();
    fixture.detectChanges();
    expect(button.disabled).toBe(false);
    const attempts = Array.from(
      fixture.nativeElement.querySelectorAll('[data-testid="knowledge-attempt"]'),
    ).map((element) => (element as HTMLElement).textContent);
    expect(attempts[0]).toContain('d84b2fb0-3436-4c61-afdf-a673535fc6cc');
    expect(attempts[1]).toContain('a74f88ed-e295-4caf-9404-a22f733d86ec');
  });

  it('showsRetryableKnowledgeApiFailureAndKeepsKnowledgeAfterPostFailure', () => {
    response = of(investigation());
    knowledgeHistoryResponse = throwError(() => new ApiRequestError('unavailable', 503));
    const fixture = TestBed.createComponent(InvestigationWorkspaceComponent);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain(
      'We couldn’t load approved knowledge history',
    );

    knowledgeHistoryResponse = of([knowledge('AVAILABLE')]);
    fixture.nativeElement.querySelector('[data-testid="retry-knowledge-history"]').click();
    fixture.detectChanges();
    expect(api.getKnowledgeHistory).toHaveBeenCalledTimes(2);

    retrievalResponse = throwError(() => new ApiRequestError('unavailable', 503));
    fixture.nativeElement.querySelector('[data-testid="retrieve-knowledge"]').click();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain(
      'Knowledge retrieval could not be completed',
    );
    expect(fixture.nativeElement.textContent).toContain('Authorization Decline Runbook');
  });

  it('keepsApprovedKnowledgeAfterObservedEvidenceAndSeparateFromAiInference', () => {
    response = of(investigation());
    knowledgeHistoryResponse = of([knowledge('AVAILABLE')]);
    const fixture = TestBed.createComponent(InvestigationWorkspaceComponent);
    fixture.detectChanges();

    const evidencePanel = fixture.nativeElement.querySelector('.evidence-panel') as HTMLElement;
    const knowledgePanel = fixture.nativeElement.querySelector(
      '[data-testid="knowledge-panel"]',
    ) as HTMLElement;
    expect(
      evidencePanel.compareDocumentPosition(knowledgePanel) & Node.DOCUMENT_POSITION_FOLLOWING,
    ).not.toBe(0);
    expect(knowledgePanel.textContent).toContain(
      'No AI inference or recommendation has been generated',
    );
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

  function knowledge(
    status: KnowledgeRetrievalStatus,
    statusDetail: string | null = null,
    results = knowledgeResults(),
  ): KnowledgeRetrieval {
    const requestedAt = '2026-08-28T10:00:00Z';
    return {
      retrievalId: 'a74f88ed-e295-4caf-9404-a22f733d86ec',
      status,
      requestedAt,
      completedAt: status === 'STARTED' ? null : '2026-08-28T10:00:02Z',
      queryText: 'Incident type: AUTHORIZATION_DECLINE_RATE_SPIKE',
      queryTemplateVersion: 'knowledge-query/v1',
      contributingEvidenceIds: ['a8bab9d4-dccc-4e70-acfe-174ac63a3b12'],
      embeddingModelId: 'amazon.titan-embed-text-v2:0',
      embeddingDimensions: 1024,
      metadataFilters: {
        incidentFamily: 'AUTHORIZATION_DECLINE_RATE_SPIKE',
        documentTypes: ['RUNBOOK', 'POLICY'],
        approvalStatus: 'APPROVED',
        effectiveAt: requestedAt,
      },
      rankingVersion: 'postgres-hybrid-rrf/v1',
      rrfK: 60,
      candidateDepth: 20,
      minimumLexicalRank: 0,
      minimumVectorSimilarity: 0.55,
      statusDetail,
      results,
    };
  }

  function knowledgeResults(): KnowledgeRetrieval['results'] {
    const base = {
      selectedPosition: 1,
      lexicalRank: 0.5,
      lexicalPosition: 1,
      vectorSimilarity: 1,
      vectorDistance: 0,
      vectorPosition: 1,
      fusedPosition: 1,
      fusedScore: 0.0327,
      documentVersion: '1.0.0',
      appliesTo: 'Card authorization',
      rawContent: 'Inspect GATEWAY_TIMEOUT observations.',
      sourceStartLine: 20,
      sourceEndLine: 22,
      approvalStatus: 'APPROVED' as const,
      approvedBy: '7b636625-53d1-46f7-92a9-9c8c27a243d1',
      approvedAt: '2026-08-20T10:00:00Z',
      effectiveAt: '2026-08-21T00:00:00Z',
    };
    return [
      {
        ...base,
        chunkId: '21111111-1111-4111-8111-111111111111',
        documentVersionId: '11111111-1111-4111-8111-111111111111',
        documentId: '31111111-1111-4111-8111-111111111111',
        documentType: 'RUNBOOK',
        documentTitle: 'Authorization Decline Runbook',
        sectionPath: 'Gateway Failures > Diagnosis',
      },
      {
        ...base,
        selectedPosition: 2,
        fusedPosition: 2,
        chunkId: '22222222-2222-4222-8222-222222222222',
        documentVersionId: '12222222-2222-4222-8222-222222222222',
        documentId: '32222222-2222-4222-8222-222222222222',
        documentType: 'POLICY',
        documentTitle: 'Payment Incident Response Policy',
        sectionPath: 'Human review',
      },
    ];
  }
});
