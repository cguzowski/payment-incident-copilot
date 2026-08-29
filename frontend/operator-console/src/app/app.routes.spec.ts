import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { of } from 'rxjs';
import { IncidentDetailApiService } from './features/incident-detail/incident-detail-api.service';
import { IncidentDetailComponent } from './features/incident-detail/incident-detail.component';
import { InvestigationApiService } from './features/investigation-workspace/investigation-api.service';
import { InvestigationWorkspaceComponent } from './features/investigation-workspace/investigation-workspace.component';
import { routes } from './app.routes';

describe('App routes', () => {
  it('loadsDetailComponentForIncidentRoute', async () => {
    await TestBed.configureTestingModule({
      providers: [
        provideRouter(routes),
        {
          provide: IncidentDetailApiService,
          useValue: {
            getDetail: vi.fn(() =>
              of({
                incidentId: 'f4749ecb-49b0-4277-a140-cb69485b082f',
                externalAlertId: 'alert-auth-decline-001',
                incidentType: 'AUTHORIZATION_DECLINE_RATE_SPIKE',
                severity: 'HIGH',
                status: 'NEW',
                title: 'Authorization decline rate above threshold',
                description: 'Synthetic authorization declines exceeded 25% for five minutes.',
                detectedAt: '2026-08-22T07:14:00Z',
                receivedAt: '2026-08-22T07:15:00Z',
                activeInvestigationId: null,
              }),
            ),
          },
        },
        { provide: InvestigationApiService, useValue: { start: vi.fn() } },
      ],
    }).compileComponents();
    const harness = await RouterTestingHarness.create();

    const component = await harness.navigateByUrl(
      '/incidents/f4749ecb-49b0-4277-a140-cb69485b082f',
      IncidentDetailComponent,
    );

    expect(component).toBeInstanceOf(IncidentDetailComponent);
    expect(harness.routeNativeElement?.textContent).toContain(
      'Authorization decline rate above threshold',
    );
  });

  it('loadsInvestigationWorkspaceOnDirectRoute', async () => {
    await TestBed.configureTestingModule({
      providers: [
        provideRouter(routes),
        {
          provide: InvestigationApiService,
          useValue: {
            get: vi.fn(() => of(investigation())),
            getEvidenceHistory: vi.fn(() => of([])),
            getKnowledgeHistory: vi.fn(() => of([])),
          },
        },
      ],
    }).compileComponents();
    const harness = await RouterTestingHarness.create();

    const component = await harness.navigateByUrl(
      '/investigations/a012c9cb-85a6-4d77-9703-3b53377b56c3',
      InvestigationWorkspaceComponent,
    );

    expect(component).toBeInstanceOf(InvestigationWorkspaceComponent);
    expect(harness.routeNativeElement?.textContent).toContain(
      'Evidence collection has not started',
    );
  });

  function investigation() {
    return {
      investigationId: 'a012c9cb-85a6-4d77-9703-3b53377b56c3',
      incidentId: 'f4749ecb-49b0-4277-a140-cb69485b082f',
      incidentStatus: 'INVESTIGATING',
      startedBy: '7b636625-53d1-46f7-92a9-9c8c27a243d1',
      startedAt: '2026-08-27T18:30:00Z',
    };
  }
});
