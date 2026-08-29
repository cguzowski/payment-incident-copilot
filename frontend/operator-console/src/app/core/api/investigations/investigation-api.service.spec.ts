import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { InvestigationApiService } from './investigation-api.service';

describe('InvestigationApiService', () => {
  let service: InvestigationApiService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [InvestigationApiService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(InvestigationApiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('startsInvestigationWithoutIdentityInTheResourceRequest', () => {
    service.start('incident/id').subscribe();
    const request = http.expectOne('/api/incidents/incident%2Fid/investigations');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toBeNull();
    expect(request.request.params.keys()).toEqual([]);
    request.flush({});
  });

  it('requestsInvestigationWorkspaceWithoutIdentityParameters', () => {
    service.get('investigation/id').subscribe();
    const request = http.expectOne('/api/investigations/investigation%2Fid');
    expect(request.request.method).toBe('GET');
    expect(request.request.params.keys()).toEqual([]);
    request.flush({});
  });
});
