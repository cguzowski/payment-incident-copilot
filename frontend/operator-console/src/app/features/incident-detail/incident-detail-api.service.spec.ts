import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { IncidentDetailApiService } from './incident-detail-api.service';

describe('IncidentDetailApiService', () => {
  let service: IncidentDetailApiService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [IncidentDetailApiService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(IncidentDetailApiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('requestsIncidentDetailWithIncidentIdAndConfiguredTenantId', () => {
    service.getDetail('incident/id with spaces').subscribe();

    const request = http.expectOne('/api/incidents/incident%2Fid%20with%20spaces');
    expect(request.request.method).toBe('GET');
    request.flush({});
  });
});
