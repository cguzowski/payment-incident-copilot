import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ApprovedKnowledgeApiService } from './approved-knowledge-api.service';

describe('ApprovedKnowledgeApiService', () => {
  let service: ApprovedKnowledgeApiService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [ApprovedKnowledgeApiService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(ApprovedKnowledgeApiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('requestsKnowledgeHistoryWithoutIdentityParameters', () => {
    service.getHistory('investigation/id').subscribe();
    const request = http.expectOne('/api/investigations/investigation%2Fid/knowledge-retrievals');
    expect(request.request.method).toBe('GET');
    request.flush([]);
  });

  it('startsTenantScopedRetrievalWithoutARequestBody', () => {
    service.retrieve('investigation/id').subscribe();
    const request = http.expectOne('/api/investigations/investigation%2Fid/knowledge-retrievals');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toBeNull();
    request.flush({});
  });
});
