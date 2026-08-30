import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Observable, Subject, of, throwError } from 'rxjs';
import { AlertQueueApiService } from './alert-queue-api.service';
import { AlertQueueComponent } from './alert-queue.component';
import { AlertQueueItem } from './alert-queue.models';

describe('AlertQueueComponent', () => {
  let queueResponse: Observable<AlertQueueItem[]>;
  let api: { getQueue: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    queueResponse = new Subject<AlertQueueItem[]>().asObservable();
    api = { getQueue: vi.fn(() => queueResponse) };
    await TestBed.configureTestingModule({
      imports: [AlertQueueComponent],
      providers: [{ provide: AlertQueueApiService, useValue: api }, provideRouter([])],
    }).compileComponents();
  });

  it('shows loading, empty, and retryable error states', () => {
    const loading = TestBed.createComponent(AlertQueueComponent);
    loading.detectChanges();
    expect(loading.nativeElement.querySelector('[role="status"]').textContent).toContain('Loading');

    queueResponse = of([]);
    const empty = TestBed.createComponent(AlertQueueComponent);
    empty.detectChanges();
    expect(empty.nativeElement.querySelector('[data-testid="empty-state"]').textContent).toContain(
      'No active incidents',
    );

    queueResponse = throwError(() => new Error('unavailable'));
    const error = TestBed.createComponent(AlertQueueComponent);
    error.detectChanges();
    const retry = error.nativeElement.querySelector('[data-testid="retry"]') as HTMLButtonElement;
    expect(retry.classList).toContain('button');
    expect(retry.classList).toContain('button--primary');
  });

  it('sortsNewestReceivedByDefaultAndLinksToIncidentDetail', () => {
    queueResponse = of(queueItems());
    const fixture = TestBed.createComponent(AlertQueueComponent);
    fixture.detectChanges();

    expect(firstRow(fixture).textContent).toContain('Gateway cohort decline spike');
    expect(
      (
        fixture.nativeElement.querySelector('[data-testid="incident-link"]') as HTMLAnchorElement
      ).getAttribute('href'),
    ).toBe('/incidents/f4749ecb-49b0-4277-a140-cb69485b082f');
  });

  it('showsReceivedAndDetectedTimestampsThatExplainDefaultOrdering', () => {
    queueResponse = of(queueItems());
    const fixture = TestBed.createComponent(AlertQueueComponent);
    fixture.detectChanges();

    const headings = Array.from(
      fixture.nativeElement.querySelectorAll('th') as NodeListOf<HTMLElement>,
      (heading) => heading.textContent?.trim(),
    );
    const rows = fixture.nativeElement.querySelectorAll(
      '[data-testid="queue-row"]',
    ) as NodeListOf<HTMLElement>;

    expect(headings).toContain('Received');
    expect(headings).toContain('Detected');
    expect(rows[0].querySelector('[data-testid="received-at"]')?.getAttribute('datetime')).toBe(
      '2026-08-22T07:20:00Z',
    );
    expect(rows[0].querySelector('[data-testid="detected-at"]')?.getAttribute('datetime')).toBe(
      '2026-08-22T07:14:00Z',
    );
    expect(rows[1].querySelector('[data-testid="received-at"]')?.getAttribute('datetime')).toBe(
      '2026-08-22T07:16:00Z',
    );
  });

  it('usesIntentionalQueueControlAndActionStyles', () => {
    queueResponse = of(queueItems());
    const fixture = TestBed.createComponent(AlertQueueComponent);
    fixture.detectChanges();

    const refresh = fixture.nativeElement.querySelector(
      '[data-testid="refresh"]',
    ) as HTMLButtonElement;
    const resume = fixture.nativeElement.querySelector(
      '[data-testid="resume-investigation"]',
    ) as HTMLAnchorElement;
    const statusActions = resume.closest('[data-testid="status-actions"]');

    expect(refresh.classList).toContain('button');
    expect(refresh.classList).toContain('button--secondary');
    expect(resume.classList).toContain('action-link');
    expect(statusActions).not.toBeNull();
  });

  it('sortsByEveryChosenQueueField', () => {
    queueResponse = of(queueItems());
    const fixture = TestBed.createComponent(AlertQueueComponent);
    fixture.detectChanges();
    const sort = fixture.nativeElement.querySelector('#queue-sort') as HTMLSelectElement;
    const expected: Record<string, string> = {
      'received-desc': 'Gateway cohort decline spike',
      'received-asc': 'Issuer decline rate elevated',
      'detected-desc': 'Issuer decline rate elevated',
      severity: 'Gateway cohort decline spike',
      status: 'Issuer decline rate elevated',
    };

    for (const [value, title] of Object.entries(expected)) {
      sort.value = value;
      sort.dispatchEvent(new Event('change'));
      fixture.detectChanges();
      expect(firstRow(fixture).textContent).toContain(title);
    }
  });

  it('refreshesWithoutResettingSort', () => {
    queueResponse = of(queueItems());
    const fixture = TestBed.createComponent(AlertQueueComponent);
    fixture.detectChanges();
    const sort = fixture.nativeElement.querySelector('#queue-sort') as HTMLSelectElement;
    sort.value = 'severity';
    sort.dispatchEvent(new Event('change'));

    (fixture.nativeElement.querySelector('[data-testid="refresh"]') as HTMLButtonElement).click();
    fixture.detectChanges();

    expect(api.getQueue).toHaveBeenCalledTimes(2);
    expect(sort.value).toBe('severity');
  });

  it('automaticallyRefreshesTheCurrentViewAndStopsWhenDestroyed', () => {
    vi.useFakeTimers();
    try {
      queueResponse = of([queueItems()[0]]);
      const fixture = TestBed.createComponent(AlertQueueComponent);
      fixture.detectChanges();
      expect(fixture.nativeElement.querySelectorAll('[data-testid="queue-row"]')).toHaveLength(1);

      const refreshedQueue = new Subject<AlertQueueItem[]>();
      queueResponse = refreshedQueue.asObservable();
      vi.advanceTimersByTime(5_000);
      fixture.detectChanges();

      expect(api.getQueue).toHaveBeenCalledTimes(2);
      expect(api.getQueue).toHaveBeenLastCalledWith('active');
      expect(fixture.nativeElement.querySelectorAll('[data-testid="queue-row"]')).toHaveLength(1);

      refreshedQueue.next(queueItems());
      fixture.detectChanges();
      expect(fixture.nativeElement.querySelectorAll('[data-testid="queue-row"]')).toHaveLength(2);

      fixture.destroy();
      vi.advanceTimersByTime(5_000);
      expect(api.getQueue).toHaveBeenCalledTimes(2);
    } finally {
      vi.useRealTimers();
    }
  });

  it('keepsThePopulatedQueueVisibleWhenAutomaticRefreshFails', () => {
    vi.useFakeTimers();
    try {
      queueResponse = of([queueItems()[0]]);
      const fixture = TestBed.createComponent(AlertQueueComponent);
      fixture.detectChanges();

      queueResponse = throwError(() => new Error('temporarily unavailable'));
      vi.advanceTimersByTime(5_000);
      fixture.detectChanges();

      expect(fixture.nativeElement.querySelectorAll('[data-testid="queue-row"]')).toHaveLength(1);
      expect(fixture.nativeElement.querySelector('[data-testid="error-state"]')).toBeNull();
      fixture.destroy();
    } finally {
      vi.useRealTimers();
    }
  });

  it('keepsInvestigatingIncidentInQueueWithResumeRoute', () => {
    queueResponse = of(queueItems());
    const fixture = TestBed.createComponent(AlertQueueComponent);
    fixture.detectChanges();

    const resume = fixture.nativeElement.querySelector(
      '[data-testid="resume-investigation"]',
    ) as HTMLAnchorElement;
    expect(resume.textContent).toContain('Resume investigation');
    expect(resume.getAttribute('href')).toBe(
      '/investigations/a012c9cb-85a6-4d77-9703-3b53377b56c3',
    );
  });

  it('keepsAwaitingReviewIncidentInQueueWithReviewRoute', () => {
    const awaiting = queueItems();
    awaiting[1] = { ...awaiting[1], status: 'AWAITING_REVIEW' };
    queueResponse = of(awaiting);
    const fixture = TestBed.createComponent(AlertQueueComponent);
    fixture.detectChanges();

    const review = fixture.nativeElement.querySelector(
      '[data-testid="resume-investigation"]',
    ) as HTMLAnchorElement;
    expect(review.textContent).toContain('Review proposed report');
    expect(review.getAttribute('href')).toBe(
      '/investigations/a012c9cb-85a6-4d77-9703-3b53377b56c3',
    );
  });

  it('keepsTerminalIncidentsDiscoverableInCompletedView', () => {
    queueResponse = of([{ ...queueItems()[1], status: 'APPROVED' }]);
    const fixture = TestBed.createComponent(AlertQueueComponent);
    fixture.detectChanges();

    (
      fixture.nativeElement.querySelector('[data-testid="completed-view"]') as HTMLButtonElement
    ).click();
    fixture.detectChanges();

    expect(api.getQueue).toHaveBeenLastCalledWith('completed');
    const link = fixture.nativeElement.querySelector(
      '[data-testid="resume-investigation"]',
    ) as HTMLAnchorElement;
    expect(link.textContent).toContain('View decision and timeline');
    expect(link.getAttribute('href')).toBe('/investigations/a012c9cb-85a6-4d77-9703-3b53377b56c3');
  });

  function firstRow(fixture: ComponentFixture<AlertQueueComponent>): HTMLElement {
    return fixture.nativeElement.querySelector('[data-testid="queue-row"]') as HTMLElement;
  }

  function queueItems(): AlertQueueItem[] {
    return [
      {
        incidentId: '20ebde75-377d-48b0-85fd-089962e16c33',
        externalAlertId: 'alert-auth-decline-001',
        incidentType: 'AUTHORIZATION_DECLINE_RATE_SPIKE',
        severity: 'HIGH',
        status: 'NEW',
        title: 'Issuer decline rate elevated',
        detectedAt: '2026-08-22T07:16:00Z',
        receivedAt: '2026-08-22T07:16:00Z',
        activeInvestigationId: null,
      },
      {
        incidentId: 'f4749ecb-49b0-4277-a140-cb69485b082f',
        externalAlertId: 'alert-auth-decline-002',
        incidentType: 'AUTHORIZATION_DECLINE_RATE_SPIKE',
        severity: 'CRITICAL',
        status: 'INVESTIGATING',
        title: 'Gateway cohort decline spike',
        detectedAt: '2026-08-22T07:14:00Z',
        receivedAt: '2026-08-22T07:20:00Z',
        activeInvestigationId: 'a012c9cb-85a6-4d77-9703-3b53377b56c3',
      },
    ];
  }
});
