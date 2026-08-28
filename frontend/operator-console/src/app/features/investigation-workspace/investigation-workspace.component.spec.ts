import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { Observable, Subject, of, throwError } from 'rxjs';
import { ApiRequestError } from '../../core/http/api-error.interceptor';
import { Investigation } from './investigation.models';
import { InvestigationApiService } from './investigation-api.service';
import { InvestigationWorkspaceComponent } from './investigation-workspace.component';

describe('InvestigationWorkspaceComponent', () => {
  let response: Observable<Investigation>;
  let api: { get: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    response = new Subject<Investigation>().asObservable();
    api = { get: vi.fn(() => response) };
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
});
