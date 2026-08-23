import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { of } from 'rxjs';
import { IncidentDetailApiService } from './features/incident-detail/incident-detail-api.service';
import { IncidentDetailComponent } from './features/incident-detail/incident-detail.component';
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
              }),
            ),
          },
        },
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
});
