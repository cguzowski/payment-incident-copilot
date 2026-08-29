import { TestBed } from '@angular/core/testing';
import { Observable, Subject, of, throwError } from 'rxjs';
import { ApiRequestError } from '../../../core/http/api-error.interceptor';
import { ObservedEvidenceApiService } from './observed-evidence-api.service';
import { ObservedEvidencePanelComponent } from './observed-evidence-panel.component';
import { EvidenceCollection, EvidenceCollectionStatus } from './observed-evidence.models';

describe('ObservedEvidencePanelComponent', () => {
  let historyResponse: Observable<EvidenceCollection[]>;
  let collectionResponse: Observable<EvidenceCollection>;
  let api: {
    getHistory: ReturnType<typeof vi.fn>;
    collect: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    historyResponse = of([]);
    collectionResponse = of(evidence('AVAILABLE'));
    api = {
      getHistory: vi.fn(() => historyResponse),
      collect: vi.fn(() => collectionResponse),
    };
    await TestBed.configureTestingModule({
      imports: [ObservedEvidencePanelComponent],
      providers: [{ provide: ObservedEvidenceApiService, useValue: api }],
    }).compileComponents();
  });

  it('loadsHistoryAndEmitsWhetherHistoryExists', () => {
    historyResponse = of([evidence('AVAILABLE')]);
    const fixture = create();
    const presence = vi.fn();
    fixture.componentInstance.historyPresence.subscribe(presence);
    fixture.detectChanges();

    expect(api.getHistory).toHaveBeenCalledWith('investigation-1');
    expect(presence).toHaveBeenCalledWith(true);
    expect(fixture.nativeElement.textContent).toContain('UPSTREAM_TIMEOUT');
    expect(fixture.nativeElement.textContent).toContain('Observed synthetic evidence');
  });

  it('preservesDistinctTerminalAndInterruptedStatuses', () => {
    const cases: [EvidenceCollectionStatus, string][] = [
      ['PARTIAL', 'Only part of the synthetic observation window was available.'],
      ['NOT_FOUND', 'No matching synthetic evidence was found.'],
      ['UNAVAILABLE', 'Evidence source unavailable.'],
      ['TIMED_OUT', 'Evidence source request timed out.'],
      ['MALFORMED', 'Tool result failed validation.'],
      ['STARTED', 'Collection did not reach a terminal status.'],
    ];
    for (const [status, detail] of cases) {
      historyResponse = of([evidence(status, status === 'STARTED' ? null : detail)]);
      const fixture = create();
      fixture.detectChanges();
      expect(fixture.nativeElement.textContent).toContain(status);
      expect(fixture.nativeElement.textContent).toContain(detail);
      fixture.destroy();
    }
  });

  it('keepsHistoryVisibleWhileCollectionIsPendingAndPrependsTheResult', () => {
    historyResponse = of([evidence('AVAILABLE')]);
    const pending = new Subject<EvidenceCollection>();
    collectionResponse = pending.asObservable();
    const fixture = create();
    fixture.detectChanges();
    const button = fixture.nativeElement.querySelector(
      '[data-testid="collect-evidence"]',
    ) as HTMLButtonElement;
    button.click();
    fixture.detectChanges();
    expect(button.disabled).toBe(true);

    pending.next({ ...evidence('UNAVAILABLE', 'Evidence source unavailable.'), evidenceId: 'new' });
    pending.complete();
    fixture.detectChanges();
    const attempts = Array.from(
      fixture.nativeElement.querySelectorAll('[data-testid="evidence-attempt"]'),
    ).map((element) => (element as HTMLElement).textContent);
    expect(attempts[0]).toContain('new');
    expect(attempts[1]).toContain('evidence-1');
  });

  it('offersIndependentHistoryRetry', () => {
    historyResponse = throwError(() => new ApiRequestError('unavailable', 503));
    const fixture = create();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('We couldn’t load evidence history');
    historyResponse = of([]);
    fixture.nativeElement.querySelector('[data-testid="retry-evidence"]').click();
    fixture.detectChanges();
    expect(api.getHistory).toHaveBeenCalledTimes(2);
  });

  function create() {
    const fixture = TestBed.createComponent(ObservedEvidencePanelComponent);
    fixture.componentRef.setInput('investigationId', 'investigation-1');
    return fixture;
  }

  function evidence(
    status: EvidenceCollectionStatus,
    statusDetail: string | null = null,
  ): EvidenceCollection {
    const hasContent = status === 'AVAILABLE' || status === 'PARTIAL';
    return {
      evidenceId: 'evidence-1',
      status,
      sourceSystem: 'synthetic-observability',
      sourceTool: 'getRecentServiceErrors',
      toolCallId: 'tool-call-1',
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
