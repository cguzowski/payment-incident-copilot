import { By } from '@angular/platform-browser';
import { TestBed } from '@angular/core/testing';
import {
  ActivatedRoute,
  convertToParamMap,
  provideRouter,
  Router,
  RouterLink,
} from '@angular/router';
import { Observable, Subject, of, throwError } from 'rxjs';
import { ApiRequestError } from '../../core/http/api-error.interceptor';
import { InvestigationApiService } from '../../core/api/investigations/investigation-api.service';
import { IncidentDetailApiService } from './incident-detail-api.service';
import { IncidentDetail } from './incident-detail.models';
import { IncidentDetailComponent } from './incident-detail.component';

describe('IncidentDetailComponent', () => {
  const incidentId = 'f4749ecb-49b0-4277-a140-cb69485b082f';
  let detailResponse: Observable<IncidentDetail>;
  let api: { getDetail: ReturnType<typeof vi.fn> };
  let investigationApi: { start: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    detailResponse = new Subject<IncidentDetail>().asObservable();
    api = { getDetail: vi.fn(() => detailResponse) };
    investigationApi = { start: vi.fn() };

    await TestBed.configureTestingModule({
      imports: [IncidentDetailComponent],
      providers: [
        { provide: IncidentDetailApiService, useValue: api },
        { provide: InvestigationApiService, useValue: investigationApi },
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: convertToParamMap({ incidentId }) } },
        },
      ],
    }).compileComponents();
  });

  it('showsLoadingStateWhileDetailRequestIsPending', () => {
    const fixture = TestBed.createComponent(IncidentDetailComponent);

    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[role="status"]').textContent).toContain(
      'Loading incident',
    );
    expect(api.getDetail).toHaveBeenCalledWith(incidentId);
  });

  it('rendersFullIncidentDetailOnSuccess', () => {
    detailResponse = of(incidentDetail());
    const fixture = TestBed.createComponent(IncidentDetailComponent);

    fixture.detectChanges();

    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Authorization decline rate above threshold');
    expect(text).toContain('Synthetic authorization declines exceeded 25% for five minutes.');
    expect(text).toContain('alert-auth-decline-001');
    expect(text).toContain('Authorization decline spike');
    expect(text).toContain('HIGH');
    expect(text).toContain('NEW');
    expect(
      fixture.nativeElement.querySelector('time[datetime="2026-08-22T07:14:00Z"]'),
    ).not.toBeNull();
    expect(
      fixture.nativeElement.querySelector('time[datetime="2026-08-22T07:15:00Z"]'),
    ).not.toBeNull();
  });

  it('showsNotFoundStateForHttp404', () => {
    detailResponse = throwError(() => new ApiRequestError('not found', 404));
    const fixture = TestBed.createComponent(IncidentDetailComponent);

    fixture.detectChanges();

    const notFound = fixture.nativeElement.querySelector(
      '[data-testid="not-found-state"]',
    ) as HTMLElement;
    expect(notFound.textContent).toContain('Incident not found');
    expect(fixture.nativeElement.querySelector('[data-testid="retry"]')).toBeNull();
  });

  it('showsRetryableErrorStateForOtherFailures', () => {
    detailResponse = throwError(() => new ApiRequestError('unavailable', 503));
    const fixture = TestBed.createComponent(IncidentDetailComponent);

    fixture.detectChanges();

    const error = fixture.nativeElement.querySelector('[role="alert"]') as HTMLElement;
    const retry = fixture.nativeElement.querySelector('[data-testid="retry"]') as HTMLButtonElement;
    expect(error.textContent).toContain('We couldn’t load this incident');
    expect(retry.classList).toContain('button');
    expect(retry.classList).toContain('button--primary');
  });

  it('retriesTheDetailRequest', () => {
    detailResponse = throwError(() => new ApiRequestError('unavailable', 503));
    const fixture = TestBed.createComponent(IncidentDetailComponent);
    const router = TestBed.inject(Router);
    fixture.detectChanges();
    const initialUrl = router.url;

    detailResponse = of(incidentDetail());
    (fixture.nativeElement.querySelector('[data-testid="retry"]') as HTMLButtonElement).click();
    fixture.detectChanges();

    expect(api.getDetail).toHaveBeenCalledTimes(2);
    expect(api.getDetail).toHaveBeenLastCalledWith(incidentId);
    expect(router.url).toBe(initialUrl);
    expect(fixture.nativeElement.textContent).toContain(
      'Authorization decline rate above threshold',
    );
  });

  it('linksBackToWorkQueue', () => {
    detailResponse = of(incidentDetail());
    const fixture = TestBed.createComponent(IncidentDetailComponent);
    fixture.detectChanges();

    const debugLink = fixture.debugElement.query(By.css('[data-testid="back-to-queue"]'));
    const routerLink = debugLink.injector.get(RouterLink);
    expect((debugLink.nativeElement as HTMLAnchorElement).textContent).toContain(
      'Back to work queue',
    );
    expect((debugLink.nativeElement as HTMLAnchorElement).classList).toContain('action-link');
    expect(routerLink.urlTree?.toString()).toBe('/');
  });

  it('startsNewIncidentWithConfiguredTenantAndOperator', () => {
    detailResponse = of(incidentDetail());
    investigationApi.start.mockReturnValue(of(investigation()));
    const fixture = TestBed.createComponent(IncidentDetailComponent);
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    fixture.detectChanges();

    const start = fixture.nativeElement.querySelector(
      '[data-testid="start-investigation"]',
    ) as HTMLButtonElement;
    expect(start.classList).toContain('button');
    expect(start.classList).toContain('button--primary');
    start.click();
    fixture.detectChanges();

    expect(investigationApi.start).toHaveBeenCalledWith(incidentId);
    expect(navigate).toHaveBeenCalledWith([
      '/investigations',
      'a012c9cb-85a6-4d77-9703-3b53377b56c3',
    ]);
  });

  it('disablesStartWhileRequestIsPending', () => {
    detailResponse = of(incidentDetail());
    investigationApi.start.mockReturnValue(new Subject().asObservable());
    const fixture = TestBed.createComponent(IncidentDetailComponent);
    fixture.detectChanges();
    const start = fixture.nativeElement.querySelector(
      '[data-testid="start-investigation"]',
    ) as HTMLButtonElement;

    start.click();
    fixture.detectChanges();

    expect(start.disabled).toBe(true);
    expect(start.textContent).toContain('Starting');
  });

  it('showsRetryableStartFailure', () => {
    detailResponse = of(incidentDetail());
    investigationApi.start.mockReturnValue(
      throwError(() => new ApiRequestError('unavailable', 503)),
    );
    const fixture = TestBed.createComponent(IncidentDetailComponent);
    fixture.detectChanges();

    (
      fixture.nativeElement.querySelector(
        '[data-testid="start-investigation"]',
      ) as HTMLButtonElement
    ).click();
    fixture.detectChanges();

    expect(
      fixture.nativeElement.querySelector('[data-testid="start-error"]').textContent,
    ).toContain('couldn’t start');
  });

  it('resumesExistingInvestigation', () => {
    detailResponse = of({
      ...incidentDetail(),
      status: 'INVESTIGATING',
      activeInvestigationId: 'a012c9cb-85a6-4d77-9703-3b53377b56c3',
    });
    const fixture = TestBed.createComponent(IncidentDetailComponent);
    fixture.detectChanges();

    const resume = fixture.nativeElement.querySelector(
      '[data-testid="resume-investigation"]',
    ) as HTMLAnchorElement;
    expect(resume.getAttribute('href')).toBe(
      '/investigations/a012c9cb-85a6-4d77-9703-3b53377b56c3',
    );
    expect(fixture.nativeElement.querySelector('[data-testid="start-investigation"]')).toBeNull();
  });

  it('usesCompactDetailWorkflowActionStyles', () => {
    detailResponse = of({
      ...incidentDetail(),
      status: 'INVESTIGATING',
      activeInvestigationId: 'a012c9cb-85a6-4d77-9703-3b53377b56c3',
    });
    const fixture = TestBed.createComponent(IncidentDetailComponent);
    fixture.detectChanges();

    const action = fixture.nativeElement.querySelector(
      '[data-testid="workflow-action"]',
    ) as HTMLElement;
    const resume = fixture.nativeElement.querySelector(
      '[data-testid="resume-investigation"]',
    ) as HTMLAnchorElement;

    expect(action.classList).toContain('workflow-action');
    expect(action.classList).not.toContain('state-card');
    expect(resume.classList).toContain('button');
    expect(resume.classList).toContain('button--primary');
  });

  function incidentDetail(): IncidentDetail {
    return {
      incidentId,
      externalAlertId: 'alert-auth-decline-001',
      incidentType: 'AUTHORIZATION_DECLINE_RATE_SPIKE',
      severity: 'HIGH',
      status: 'NEW',
      title: 'Authorization decline rate above threshold',
      description: 'Synthetic authorization declines exceeded 25% for five minutes.',
      detectedAt: '2026-08-22T07:14:00Z',
      receivedAt: '2026-08-22T07:15:00Z',
      activeInvestigationId: null,
    };
  }

  function investigation() {
    return {
      investigationId: 'a012c9cb-85a6-4d77-9703-3b53377b56c3',
      incidentId,
      incidentStatus: 'INVESTIGATING' as const,
      startedBy: '7b636625-53d1-46f7-92a9-9c8c27a243d1',
      startedAt: '2026-08-27T18:30:00Z',
    };
  }
});
