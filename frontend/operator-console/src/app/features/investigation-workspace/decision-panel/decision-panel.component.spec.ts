import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Observable, Subject, of, throwError } from 'rxjs';
import { ApiRequestError } from '../../../core/http/api-error.interceptor';
import { HumanDecision } from './decision.models';
import { DecisionApiService } from './decision-api.service';
import { DecisionPanelComponent } from './decision-panel.component';

describe('DecisionPanelComponent', () => {
  let history: Observable<HumanDecision[]>;
  let getHistory: ReturnType<typeof vi.fn>;
  let record: ReturnType<typeof vi.fn>;
  let fixture: ComponentFixture<DecisionPanelComponent>;

  beforeEach(async () => {
    history = of([]);
    getHistory = vi.fn(() => history);
    record = vi.fn(() => of(decision()));
    await TestBed.configureTestingModule({
      imports: [DecisionPanelComponent],
      providers: [
        {
          provide: DecisionApiService,
          useValue: {
            getHistory,
            record,
          },
        },
      ],
    }).compileComponents();
  });

  it('submitsExplicitOutcomeAndRequiredReasonWithoutClientOwnedIds', () => {
    fixture = createPanel();
    selectOutcome('APPROVED');
    setReason('  Reviewed evidence supports escalation.  ');

    submit();
    fixture.detectChanges();

    expect(record).toHaveBeenCalledWith('investigation-1', {
      outcome: 'APPROVED',
      reason: 'Reviewed evidence supports escalation.',
    });
    expect(fixture.nativeElement.textContent).toContain('Final human decision');
    expect(fixture.nativeElement.textContent).toContain('Reviewed evidence supports escalation.');
    expect(fixture.nativeElement.textContent).not.toContain('Undo');
    expect(fixture.nativeElement.textContent).not.toContain('Execute');
  });

  it('preventsBlankOversizedAndDuplicatePendingSubmission', () => {
    const pending = new Subject<HumanDecision>();
    record.mockReturnValue(pending.asObservable());
    fixture = createPanel();
    submit();
    expect(record).not.toHaveBeenCalled();

    selectOutcome('REJECTED');
    setReason('x'.repeat(1001));
    submit();
    expect(record).not.toHaveBeenCalled();

    setReason('Insufficient evidence.');
    submit();
    submit();
    expect(record).toHaveBeenCalledTimes(1);
    expect(
      (fixture.nativeElement.querySelector('[data-testid="record-decision"]') as HTMLButtonElement)
        .disabled,
    ).toBe(true);
  });

  it('rendersStoredDecisionAsReadOnlyAfterDirectRefresh', () => {
    history = of([decision()]);
    fixture = createPanel('APPROVED');

    expect(fixture.nativeElement.textContent).toContain('APPROVED');
    expect(fixture.nativeElement.textContent).toContain('report-1');
    expect(fixture.nativeElement.querySelector('form')).toBeNull();
  });

  it.each([
    [400, 'Check the outcome and reason'],
    [404, 'Investigation not found'],
    [503, 'couldn’t record the decision'],
  ])('recoversFromHttpError %s', (status, message) => {
    record.mockReturnValue(throwError(() => new ApiRequestError('failed', status)));
    fixture = createPanel();
    selectOutcome('REJECTED');
    setReason('Evidence is insufficient.');
    submit();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[role="alert"]').textContent).toContain(message);
    expect(
      (fixture.nativeElement.querySelector('[data-testid="record-decision"]') as HTMLButtonElement)
        .disabled,
    ).toBe(false);
  });

  it('reloadsAuthoritativeDecisionAndRequestsInvestigationRefreshOnConflict', () => {
    record.mockReturnValue(throwError(() => new ApiRequestError('conflict', 409)));
    fixture = createPanel();
    history = of([decision()]);
    const refresh = vi.fn();
    fixture.componentInstance.refreshRequested.subscribe(refresh);
    selectOutcome('APPROVED');
    setReason('Reviewed evidence supports escalation.');
    submit();
    fixture.detectChanges();

    expect(refresh).toHaveBeenCalledOnce();
    expect(getHistory).toHaveBeenCalledTimes(2);
    expect(fixture.nativeElement.querySelector('[data-testid="stored-decision"]')).not.toBeNull();
  });

  function createPanel(status: 'AWAITING_REVIEW' | 'APPROVED' = 'AWAITING_REVIEW') {
    const created = TestBed.createComponent(DecisionPanelComponent);
    created.componentRef.setInput('investigationId', 'investigation-1');
    created.componentRef.setInput('incidentStatus', status);
    created.detectChanges();
    return created;
  }

  function selectOutcome(value: string): void {
    const input = fixture.nativeElement.querySelector(
      `input[value="${value}"]`,
    ) as HTMLInputElement;
    input.click();
    fixture.detectChanges();
  }

  function setReason(value: string): void {
    const textarea = fixture.nativeElement.querySelector('textarea') as HTMLTextAreaElement;
    textarea.value = value;
    textarea.dispatchEvent(new Event('input'));
    fixture.detectChanges();
  }

  function submit(): void {
    (fixture.nativeElement.querySelector('form') as HTMLFormElement).dispatchEvent(
      new Event('submit'),
    );
    fixture.detectChanges();
  }

  function decision(): HumanDecision {
    return {
      decisionId: 'decision-1',
      investigationId: 'investigation-1',
      reportAttemptId: 'report-1',
      outcome: 'APPROVED',
      incidentStatus: 'APPROVED',
      reason: 'Reviewed evidence supports escalation.',
      decidedBy: 'operator-1',
      decidedAt: '2026-08-30T09:08:00Z',
    };
  }
});
