import { TestBed } from '@angular/core/testing';
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

  it('shows a loading state while the queue request is pending', () => {
    const fixture = TestBed.createComponent(AlertQueueComponent);

    fixture.detectChanges();

    const status = fixture.nativeElement.querySelector('[role="status"]') as HTMLElement;
    expect(status.textContent).toContain('Loading alert queue');
  });

  it('shows the empty state when there are no new incidents', () => {
    queueResponse = of([]);
    const fixture = TestBed.createComponent(AlertQueueComponent);

    fixture.detectChanges();

    const emptyState = fixture.nativeElement.querySelector(
      '[data-testid="empty-state"]',
    ) as HTMLElement;
    expect(emptyState.textContent).toContain('No new incidents');
    expect(fixture.nativeElement.querySelectorAll('[data-testid="queue-row"]')).toHaveLength(0);
  });

  it('renders the triage fields returned by a successful queue request', () => {
    queueResponse = of(queueItems());
    const fixture = TestBed.createComponent(AlertQueueComponent);

    fixture.detectChanges();

    const rows = fixture.nativeElement.querySelectorAll(
      '[data-testid="queue-row"]',
    ) as NodeListOf<HTMLElement>;
    expect(rows).toHaveLength(2);
    expect(fixture.nativeElement.textContent).toContain('Gateway cohort decline spike');
    expect(fixture.nativeElement.textContent).toContain('CRITICAL');
    expect(fixture.nativeElement.textContent).toContain('NEW');
    expect(fixture.nativeElement.textContent).toContain('alert-auth-decline-002');
  });

  it('queueIncidentTitleLinksToIncidentDetailRoute', () => {
    queueResponse = of(queueItems());
    const fixture = TestBed.createComponent(AlertQueueComponent);

    fixture.detectChanges();

    const link = fixture.nativeElement.querySelector(
      '[data-testid="incident-link"]',
    ) as HTMLAnchorElement;
    expect(link.tagName).toBe('A');
    expect(link.textContent?.trim()).toBe('Issuer decline rate elevated');
    expect(link.getAttribute('href')).toBe('/incidents/20ebde75-377d-48b0-85fd-089962e16c33');
  });

  it('shows an error state and retries the request', () => {
    queueResponse = throwError(() => new Error('network unavailable'));
    const fixture = TestBed.createComponent(AlertQueueComponent);
    fixture.detectChanges();

    const errorState = fixture.nativeElement.querySelector('[role="alert"]') as HTMLElement;
    expect(errorState.textContent).toContain('We couldn’t load the alert queue');

    queueResponse = of([]);
    const retry = fixture.nativeElement.querySelector('[data-testid="retry"]') as HTMLButtonElement;
    retry.click();
    fixture.detectChanges();

    expect(api.getQueue).toHaveBeenCalledTimes(2);
    expect(fixture.nativeElement.querySelector('[data-testid="empty-state"]')).not.toBeNull();
  });

  it('lets the operator sort incidents by severity', () => {
    queueResponse = of(queueItems());
    const fixture = TestBed.createComponent(AlertQueueComponent);
    fixture.detectChanges();

    const sort = fixture.nativeElement.querySelector('#queue-sort') as HTMLSelectElement;
    sort.value = 'severity';
    sort.dispatchEvent(new Event('change'));
    fixture.detectChanges();

    const rows = fixture.nativeElement.querySelectorAll(
      '[data-testid="queue-row"]',
    ) as NodeListOf<HTMLElement>;
    expect(rows[0].textContent).toContain('Gateway cohort decline spike');
    expect(rows[1].textContent).toContain('Issuer decline rate elevated');
  });

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
      },
      {
        incidentId: 'f4749ecb-49b0-4277-a140-cb69485b082f',
        externalAlertId: 'alert-auth-decline-002',
        incidentType: 'AUTHORIZATION_DECLINE_RATE_SPIKE',
        severity: 'CRITICAL',
        status: 'NEW',
        title: 'Gateway cohort decline spike',
        detectedAt: '2026-08-22T07:14:00Z',
        receivedAt: '2026-08-22T07:15:00Z',
      },
    ];
  }
});
