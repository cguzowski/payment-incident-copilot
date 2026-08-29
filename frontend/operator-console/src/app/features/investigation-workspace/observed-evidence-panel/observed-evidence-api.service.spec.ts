import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ObservedEvidenceApiService } from './observed-evidence-api.service';

describe('ObservedEvidenceApiService', () => {
  let service: ObservedEvidenceApiService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [ObservedEvidenceApiService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(ObservedEvidenceApiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('requestsEvidenceHistoryWithoutIdentityParameters', () => {
    service.getHistory('investigation/id').subscribe();
    const request = http.expectOne('/api/investigations/investigation%2Fid/evidence-collections');
    expect(request.request.method).toBe('GET');
    request.flush([]);
  });

  it('startsTenantScopedCollectionWithoutARequestBody', () => {
    service.collect('investigation/id').subscribe();
    const request = http.expectOne('/api/investigations/investigation%2Fid/evidence-collections');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toBeNull();
    request.flush({});
  });
});
