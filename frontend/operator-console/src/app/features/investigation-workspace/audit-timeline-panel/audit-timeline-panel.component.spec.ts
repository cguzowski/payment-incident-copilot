import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { ApiRequestError } from '../../../core/http/api-error.interceptor';
import { AuditTimelineApiService } from './audit-timeline-api.service';
import { AuditTimelinePanelComponent } from './audit-timeline-panel.component';

describe('AuditTimelinePanelComponent', () => {
  let getTimeline: ReturnType<typeof vi.fn>;

  beforeEach(async () => {
    getTimeline = vi.fn(() => of([event()]));
    await TestBed.configureTestingModule({
      imports: [AuditTimelinePanelComponent],
      providers: [{ provide: AuditTimelineApiService, useValue: { getTimeline } }],
    }).compileComponents();
  });

  it('loadsIndependentlyAndRendersUnattributedEventsWithoutInventingAnActor', () => {
    const fixture = createPanel();
    expect(fixture.nativeElement.querySelector('ol')).not.toBeNull();
    expect(fixture.nativeElement.textContent).toContain('Evidence collection');
    expect(fixture.nativeElement.textContent).toContain('UNATTRIBUTED');
    expect(fixture.nativeElement.textContent).not.toContain('System operator');
    expect(fixture.nativeElement.textContent).toContain('source-1');
  });

  it('supportsEmptyNotFoundAndRetryableErrorStates', () => {
    getTimeline.mockReturnValueOnce(of([]));
    const empty = createPanel();
    expect(empty.nativeElement.textContent).toContain('No timeline events');
    empty.destroy();

    getTimeline.mockReturnValueOnce(throwError(() => new ApiRequestError('missing', 404)));
    const missing = createPanel();
    expect(missing.nativeElement.textContent).toContain('Investigation not found');
    missing.destroy();

    getTimeline.mockReturnValueOnce(throwError(() => new ApiRequestError('failed', 503)));
    const error = createPanel();
    expect(error.nativeElement.querySelector('[data-testid="retry-timeline"]')).not.toBeNull();
  });

  it('refreshesAfterTheParentRevisionChanges', () => {
    const fixture = createPanel();
    fixture.componentRef.setInput('refreshKey', 1);
    fixture.detectChanges();
    expect(getTimeline).toHaveBeenCalledTimes(2);
  });

  function createPanel() {
    const fixture = TestBed.createComponent(AuditTimelinePanelComponent);
    fixture.componentRef.setInput('investigationId', 'investigation-1');
    fixture.componentRef.setInput('refreshKey', 0);
    fixture.detectChanges();
    return fixture;
  }

  function event() {
    return {
      sourceId: 'source-1',
      eventType: 'EVIDENCE_COLLECTION' as const,
      occurredAt: '2026-08-30T09:02:00Z',
      completedAt: '2026-08-30T09:03:00Z',
      actorKind: 'UNATTRIBUTED' as const,
      actorId: null,
      status: 'AVAILABLE',
      investigationCorrelationId: 'correlation-1',
      resultingIncidentStatus: null,
      relatedSourceId: null,
      toolCallId: 'tool-1',
      modelId: null,
      promptVersion: null,
      schemaVersion: 'service-errors/v1',
      disposition: null,
      reason: null,
      description: 'Synthetic service-error evidence collection.',
    };
  }
});
