import { TestBed } from '@angular/core/testing';
import { Observable, Subject, of, throwError } from 'rxjs';
import { ApiRequestError } from '../../../core/http/api-error.interceptor';
import { ReportApiService } from './report-api.service';
import { ReportGenerationAttempt, ReportGenerationStatus } from './report.models';
import { ReportPanelComponent } from './report-panel.component';

describe('ReportPanelComponent', () => {
  let historyResponse: Observable<ReportGenerationAttempt[]>;
  let generationResponse: Observable<ReportGenerationAttempt>;
  let api: { getHistory: ReturnType<typeof vi.fn>; generate: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    historyResponse = of([]);
    generationResponse = of(attempt('AVAILABLE'));
    api = {
      getHistory: vi.fn(() => historyResponse),
      generate: vi.fn(() => generationResponse),
    };
    await TestBed.configureTestingModule({
      imports: [ReportPanelComponent],
      providers: [{ provide: ReportApiService, useValue: api }],
    }).compileComponents();
  });

  it('rendersSeparatedClaimsAndAdjacentEvidenceAndKnowledgeProvenance', () => {
    historyResponse = of([attempt('AVAILABLE')]);
    const fixture = create();
    fixture.detectChanges();
    const text = fixture.nativeElement.textContent;
    expect(text).toContain('AI-generated, advisory, and not yet reviewed');
    expect(text).toContain('Observed facts');
    expect(text).toContain('Inference');
    expect(text).toContain('Probable cause');
    expect(text).toContain('Recommendation');
    expect(text).toContain('Evidence gaps');
    expect(text).toContain('evidence-1');
    expect(text).toContain('chunk-1');
    const controls = Array.from(
      fixture.nativeElement.querySelectorAll('button'),
      (button: Element) => button.textContent?.trim(),
    );
    expect(controls).not.toContain('Approve');
    expect(controls).not.toContain('Reject');
  });

  it('loadsIndependentlyAndGeneratesWithoutClientPromptOrSources', () => {
    const fixture = create();
    fixture.detectChanges();
    expect(api.getHistory).toHaveBeenCalledWith('investigation-1');

    fixture.nativeElement.querySelector('[data-testid="generate-report"]').click();
    fixture.detectChanges();

    expect(api.generate).toHaveBeenCalledWith('investigation-1');
    expect(fixture.nativeElement.textContent).toContain('AVAILABLE');
  });

  it('disablesGenerationWhilePendingAndPreservesHistoryOnConflict', () => {
    historyResponse = of([attempt('TIMED_OUT')]);
    const pending = new Subject<ReportGenerationAttempt>();
    generationResponse = pending.asObservable();
    const fixture = create();
    fixture.detectChanges();
    const button = fixture.nativeElement.querySelector(
      '[data-testid="generate-report"]',
    ) as HTMLButtonElement;
    button.click();
    fixture.detectChanges();
    expect(button.disabled).toBe(true);
    expect(fixture.nativeElement.textContent).toContain('Generating proposed report');
    pending.error(new ApiRequestError('conflict', 409));
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Report generation is not ready');
    expect(fixture.nativeElement.textContent).toContain('TIMED_OUT');
  });

  it('distinguishesInsufficientUnavailableTimedOutMalformedAndInterruptedStates', () => {
    const cases: [ReportGenerationStatus, string][] = [
      ['UNAVAILABLE', 'The report model is unavailable.'],
      ['TIMED_OUT', 'The report model timed out.'],
      ['MALFORMED', 'The model response did not match report-v1.'],
      ['STARTED', 'may have been interrupted'],
    ];
    for (const [status, expected] of cases) {
      historyResponse = of([attempt(status)]);
      const fixture = create();
      fixture.detectChanges();
      expect(fixture.nativeElement.textContent).toContain(status);
      expect(fixture.nativeElement.textContent).toContain(expected);
      fixture.destroy();
    }

    historyResponse = of([attempt('AVAILABLE', true)]);
    const insufficient = create();
    insufficient.detectChanges();
    expect(insufficient.nativeElement.textContent).toContain('INSUFFICIENT_EVIDENCE');
    expect(insufficient.nativeElement.textContent).toContain('Low');
  });

  it('rendersIndependentLoadingNotFoundAndApiErrorStates', () => {
    historyResponse = new Subject<ReportGenerationAttempt[]>().asObservable();
    const loading = create();
    loading.detectChanges();
    expect(loading.nativeElement.querySelector('[role="status"]').textContent).toContain('Loading');
    loading.destroy();

    historyResponse = throwError(() => new ApiRequestError('not found', 404));
    const notFound = create();
    notFound.detectChanges();
    expect(notFound.nativeElement.textContent).toContain('Investigation not found');
    notFound.destroy();

    historyResponse = throwError(() => new ApiRequestError('error', 503));
    const error = create();
    error.detectChanges();
    expect(
      error.nativeElement.querySelector('[data-testid="retry-report-history"]'),
    ).not.toBeNull();
  });

  function create() {
    const fixture = TestBed.createComponent(ReportPanelComponent);
    fixture.componentRef.setInput('investigationId', 'investigation-1');
    fixture.componentRef.setInput('incidentStatus', 'INVESTIGATING');
    return fixture;
  }

  function attempt(status: ReportGenerationStatus, insufficient = false): ReportGenerationAttempt {
    return {
      attemptId: `attempt-${status}`,
      investigationId: 'investigation-1',
      status,
      requestedAt: '2026-08-29T10:00:00Z',
      completedAt: status === 'STARTED' ? null : '2026-08-29T10:00:02Z',
      modelId: 'global.amazon.nova-2-lite-v1:0',
      promptVersion: 'report-prompt/v1',
      schemaVersion: 'report-v1',
      latestEvidenceId: 'evidence-1',
      applicableEvidenceId: 'evidence-1',
      retrievalId: 'retrieval-1',
      statusDetail:
        status === 'UNAVAILABLE'
          ? 'The report model is unavailable.'
          : status === 'TIMED_OUT'
            ? 'The report model timed out.'
            : status === 'MALFORMED'
              ? 'The model response did not match report-v1.'
              : null,
      report:
        status === 'AVAILABLE'
          ? {
              disposition: insufficient ? 'INSUFFICIENT_EVIDENCE' : 'PROPOSED',
              summary: claim('Gateway timeouts correlate with declines.'),
              observations: [claim('Twelve gateway timeouts were observed.')],
              inferences: insufficient ? [] : [claim('The pattern indicates instability.', true)],
              probableCause: insufficient ? null : claim('Gateway instability is probable.', true),
              confidence: {
                level: insufficient ? 'LOW' : 'MEDIUM',
                rationale: 'One evidence source is available.',
                evidenceIds: ['evidence-1'],
              },
              recommendation: insufficient ? null : claim('Follow gateway diagnostics.', true),
              contradictions: [],
              evidenceGaps: [{ description: 'Deployment history is unavailable.' }],
            }
          : null,
    };
  }

  function claim(statement: string, knowledge = false) {
    return {
      statement,
      evidenceIds: ['evidence-1'],
      knowledgeChunkIds: knowledge ? ['chunk-1'] : [],
    };
  }
});
