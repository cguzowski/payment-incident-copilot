import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { Observable, Subject, of, throwError } from 'rxjs';
import { InvestigationApiService } from '../../core/api/investigations/investigation-api.service';
import { Investigation } from '../../core/api/investigations/investigation.models';
import { ApiRequestError } from '../../core/http/api-error.interceptor';
import { ApprovedKnowledgeApiService } from './approved-knowledge-panel/approved-knowledge-api.service';
import { ObservedEvidenceApiService } from './observed-evidence-panel/observed-evidence-api.service';
import { InvestigationWorkspaceComponent } from './investigation-workspace.component';

describe('InvestigationWorkspaceComponent', () => {
  let response: Observable<Investigation>;

  beforeEach(async () => {
    response = new Subject<Investigation>().asObservable();
    await TestBed.configureTestingModule({
      imports: [InvestigationWorkspaceComponent],
      providers: [
        { provide: InvestigationApiService, useValue: { get: vi.fn(() => response) } },
        { provide: ObservedEvidenceApiService, useValue: { getHistory: vi.fn(() => of([])) } },
        { provide: ApprovedKnowledgeApiService, useValue: { getHistory: vi.fn(() => of([])) } },
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

  it('showsRouteLevelLoadingState', () => {
    const fixture = TestBed.createComponent(InvestigationWorkspaceComponent);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[role="status"]').textContent).toContain('Loading');
  });

  it('rendersShellAndComposesEvidenceBeforeKnowledge', () => {
    response = of(investigation());
    const fixture = TestBed.createComponent(InvestigationWorkspaceComponent);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('INVESTIGATING');
    expect(
      fixture.nativeElement.querySelector('[data-testid="incident-link"]').getAttribute('href'),
    ).toBe('/incidents/f4749ecb-49b0-4277-a140-cb69485b082f');
    const evidence = fixture.nativeElement.querySelector('app-observed-evidence-panel');
    const knowledge = fixture.nativeElement.querySelector('app-approved-knowledge-panel');
    expect(evidence.compareDocumentPosition(knowledge) & Node.DOCUMENT_POSITION_FOLLOWING).not.toBe(
      0,
    );
  });

  it('updatesShellDescriptionFromEvidencePanelHistory', () => {
    response = of(investigation());
    TestBed.overrideProvider(ObservedEvidenceApiService, {
      useValue: { getHistory: vi.fn(() => of([{ evidenceId: 'evidence-1' }])) },
    });
    const fixture = TestBed.createComponent(InvestigationWorkspaceComponent);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain(
      'Service-error evidence collection is tracked below',
    );
  });

  it('showsNotFoundAndRetryableErrorStates', () => {
    response = throwError(() => new ApiRequestError('not found', 404));
    const notFound = TestBed.createComponent(InvestigationWorkspaceComponent);
    notFound.detectChanges();
    expect(notFound.nativeElement.textContent).toContain('Investigation not found');
    notFound.destroy();

    response = throwError(() => new ApiRequestError('unavailable', 503));
    const error = TestBed.createComponent(InvestigationWorkspaceComponent);
    error.detectChanges();
    expect(error.nativeElement.querySelector('[data-testid="retry"]')).not.toBeNull();
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
