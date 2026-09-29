import { TestBed } from '@angular/core/testing';
import { Observable, Subject, of, throwError } from 'rxjs';
import { ApiRequestError } from '../../../core/http/api-error.interceptor';
import { ApprovedKnowledgeApiService } from './approved-knowledge-api.service';
import { ApprovedKnowledgePanelComponent } from './approved-knowledge-panel.component';
import { KnowledgeRetrieval, KnowledgeRetrievalStatus } from './approved-knowledge.models';

describe('ApprovedKnowledgePanelComponent', () => {
  let historyResponse: Observable<KnowledgeRetrieval[]>;
  let retrievalResponse: Observable<KnowledgeRetrieval>;
  let api: {
    getHistory: ReturnType<typeof vi.fn>;
    retrieve: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    historyResponse = of([]);
    retrievalResponse = of(knowledge('AVAILABLE'));
    api = {
      getHistory: vi.fn(() => historyResponse),
      retrieve: vi.fn(() => retrievalResponse),
    };
    await TestBed.configureTestingModule({
      imports: [ApprovedKnowledgePanelComponent],
      providers: [{ provide: ApprovedKnowledgeApiService, useValue: api }],
    }).compileComponents();
  });

  it('rendersApprovedRawSourceMaterialWithProvenanceAndBoundaryCopy', () => {
    historyResponse = of([knowledge('AVAILABLE')]);
    const fixture = create();
    fixture.detectChanges();
    const text = fixture.nativeElement.textContent;
    expect(text).toContain('Authorization Decline Runbook');
    expect(text).toContain('Inspect GATEWAY_TIMEOUT observations.');
    expect(text).toContain('chunk-1');
    expect(text).toContain('rb-002-gateway-connectivity.pdf');
    expect(text).toContain('PDF page 4, blocks 2–4');
    expect(text).toContain('d'.repeat(64));
    expect(text).toContain('not an AI conclusion or executable instruction');
  });

  it('makesPopulatedKnowledgeAndEachSelectedSourceIndependentNativeDisclosures', () => {
    const attempt = knowledge('AVAILABLE');
    attempt.results = [
      attempt.results[0],
      {
        ...attempt.results[0],
        chunkId: 'chunk-2',
        documentId: 'document-2',
        documentVersionId: 'version-2',
        selectedPosition: 2,
        documentType: 'POLICY',
        documentTitle: 'Authorization Escalation Policy',
      },
    ];
    historyResponse = of([attempt]);
    const fixture = create();
    fixture.detectChanges();

    const panel = fixture.nativeElement.querySelector(
      '[data-testid="knowledge-disclosure"]',
    ) as HTMLDetailsElement;
    const sources = Array.from(
      fixture.nativeElement.querySelectorAll('[data-testid="knowledge-result-disclosure"]'),
    ) as HTMLDetailsElement[];
    expect(panel.open).toBe(true);
    expect(panel.querySelector('summary')?.textContent).toContain('Runbooks and policy');
    expect(sources).toHaveLength(2);
    expect(sources.every((source) => source.open)).toBe(true);
    expect(sources[0].querySelector('summary')?.textContent).toContain(
      'Authorization Decline Runbook',
    );
    expect(sources[1].querySelector('summary')?.textContent).toContain(
      'Authorization Escalation Policy',
    );

    sources[0].querySelector('summary')?.click();
    expect(sources[0].open).toBe(false);
    expect(sources[1].open).toBe(true);
  });

  it('linksExactRetrievalChunksEvidenceAndPdfPageInNewTabs', () => {
    historyResponse = of([knowledge('AVAILABLE')]);
    const fixture = create();
    fixture.detectChanges();

    const attempt = fixture.nativeElement.querySelector('#retrieval-retrieval-1');
    expect(
      attempt.querySelector('a[href="/investigations/investigation-1#retrieval-retrieval-1"]'),
    ).not.toBeNull();
    const chunk = fixture.nativeElement.querySelector('#knowledge-retrieval-1-chunk-1');
    expect(
      chunk.querySelector(
        'a[href="/investigations/investigation-1#knowledge-retrieval-1-chunk-1"]',
      ),
    ).not.toBeNull();
    expect(
      attempt.querySelector('a[href="/investigations/investigation-1#evidence-evidence-1"]'),
    ).not.toBeNull();
    const pdf = attempt.querySelector(
      'a[href="/api/knowledge/pdf-artifacts/' + 'd'.repeat(64) + '#page=4"]',
    );
    expect(pdf?.getAttribute('target')).toBe('_blank');
    expect(pdf?.getAttribute('rel')).toBe('noopener noreferrer');
  });

  it('keepsTheEmptyKnowledgeStateExpandedWithoutADisclosure', () => {
    const fixture = create();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-testid="knowledge-disclosure"]')).toBeNull();
    expect(fixture.nativeElement.textContent).toContain(
      'Approved knowledge has not been retrieved',
    );
    expect(
      fixture.nativeElement.querySelector('[data-testid="retrieve-knowledge"]'),
    ).not.toBeNull();
  });

  it('keepsHistoricalMarkdownLineCitationsBackwardCompatible', () => {
    const attempt = knowledge('AVAILABLE');
    attempt.results = [
      {
        ...attempt.results[0],
        sourceName: 'authorization-decline-runbook.md',
        sourceFormat: 'MARKDOWN',
        pdfSha256: null,
        sourceStartLine: 20,
        sourceEndLine: 22,
        sourceStartPage: null,
        sourceEndPage: null,
        sourceStartBlock: null,
        sourceEndBlock: null,
      },
    ];
    historyResponse = of([attempt]);

    const fixture = create();
    fixture.detectChanges();
    const text = fixture.nativeElement.textContent;
    expect(text).toContain('authorization-decline-runbook.md');
    expect(text).toContain('Source lines');
    expect(text).toContain('20–22');
    expect(text).not.toContain('PDF location');
    expect(text).not.toContain('PDF SHA-256');
    expect(
      fixture.nativeElement.querySelector('a[href^="/api/knowledge/pdf-artifacts/"]'),
    ).toBeNull();
  });

  it('preservesDistinctTerminalAndInterruptedStatuses', () => {
    const cases: [KnowledgeRetrievalStatus, string][] = [
      ['PARTIAL', 'Query embedding was unavailable; lexical retrieval was used.'],
      ['NO_MATCH', 'No eligible approved knowledge matched this investigation.'],
      ['UNAVAILABLE', 'Query embedding was unavailable and lexical retrieval returned no match.'],
      ['TIMED_OUT', 'Query embedding timed out and lexical retrieval returned no match.'],
      ['MALFORMED', 'Query embedding output was malformed.'],
      ['STARTED', 'Retrieval did not reach a terminal status.'],
    ];
    for (const [status, detail] of cases) {
      historyResponse = of([knowledge(status, status === 'NO_MATCH' ? null : detail, [])]);
      const fixture = create();
      fixture.detectChanges();
      expect(fixture.nativeElement.textContent).toContain(status);
      expect(fixture.nativeElement.textContent).toContain(detail);
      fixture.destroy();
    }
  });

  it('keepsHistoryVisibleWhileRetrievalIsPendingAndPrependsTheResult', () => {
    historyResponse = of([knowledge('AVAILABLE')]);
    const pending = new Subject<KnowledgeRetrieval>();
    retrievalResponse = pending.asObservable();
    const fixture = create();
    fixture.detectChanges();
    const button = fixture.nativeElement.querySelector(
      '[data-testid="retrieve-knowledge"]',
    ) as HTMLButtonElement;
    button.click();
    fixture.detectChanges();
    expect(button.disabled).toBe(true);

    pending.next({ ...knowledge('NO_MATCH', null, []), retrievalId: 'new' });
    pending.complete();
    fixture.detectChanges();
    const attempts = Array.from(
      fixture.nativeElement.querySelectorAll('[data-testid="knowledge-attempt"]'),
    ).map((element) => (element as HTMLElement).textContent);
    expect(attempts[0]).toContain('new');
    expect(attempts[1]).toContain('retrieval-1');
  });

  it('offersIndependentHistoryRetryAndKeepsHistoryAfterPostFailure', () => {
    historyResponse = throwError(() => new ApiRequestError('unavailable', 503));
    const fixture = create();
    fixture.detectChanges();
    historyResponse = of([knowledge('AVAILABLE')]);
    fixture.nativeElement.querySelector('[data-testid="retry-knowledge-history"]').click();
    fixture.detectChanges();
    retrievalResponse = throwError(() => new ApiRequestError('unavailable', 503));
    fixture.nativeElement.querySelector('[data-testid="retrieve-knowledge"]').click();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain(
      'Knowledge retrieval could not be completed',
    );
    expect(fixture.nativeElement.textContent).toContain('Authorization Decline Runbook');
  });

  function create() {
    const fixture = TestBed.createComponent(ApprovedKnowledgePanelComponent);
    fixture.componentRef.setInput('investigationId', 'investigation-1');
    return fixture;
  }

  function knowledge(
    status: KnowledgeRetrievalStatus,
    statusDetail: string | null = null,
    results = knowledgeResults(),
  ): KnowledgeRetrieval {
    return {
      retrievalId: 'retrieval-1',
      status,
      requestedAt: '2026-08-28T10:00:00Z',
      completedAt: status === 'STARTED' ? null : '2026-08-28T10:00:02Z',
      queryText: 'Incident type: AUTHORIZATION_DECLINE_RATE_SPIKE',
      queryTemplateVersion: 'knowledge-query/v1',
      contributingEvidenceIds: ['evidence-1'],
      embeddingModelId: 'amazon.titan-embed-text-v2:0',
      embeddingDimensions: 1024,
      metadataFilters: {
        incidentFamily: 'AUTHORIZATION_DECLINE_RATE_SPIKE',
        documentTypes: ['RUNBOOK', 'POLICY'],
        approvalStatus: 'APPROVED',
        effectiveAt: '2026-08-28T10:00:00Z',
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
    return [
      {
        chunkId: 'chunk-1',
        documentVersionId: 'version-1',
        documentId: 'document-1',
        selectedPosition: 1,
        lexicalRank: 0.5,
        lexicalPosition: 1,
        vectorSimilarity: 1,
        vectorDistance: 0,
        vectorPosition: 1,
        fusedPosition: 1,
        fusedScore: 0.0327,
        documentType: 'RUNBOOK',
        documentTitle: 'Authorization Decline Runbook',
        documentVersion: '1.0.0',
        appliesTo: 'Card authorization',
        sectionPath: 'Gateway Failures > Diagnosis',
        rawContent: 'Inspect GATEWAY_TIMEOUT observations.',
        sourceName: 'rb-002-gateway-connectivity.pdf',
        sourceFormat: 'PDF',
        pdfSha256: 'd'.repeat(64),
        sourceStartLine: null,
        sourceEndLine: null,
        sourceStartPage: 4,
        sourceEndPage: 4,
        sourceStartBlock: 2,
        sourceEndBlock: 4,
        approvalStatus: 'APPROVED',
        approvedBy: 'operator-1',
        approvedAt: '2026-08-20T10:00:00Z',
        effectiveAt: '2026-08-21T00:00:00Z',
      },
    ];
  }
});
