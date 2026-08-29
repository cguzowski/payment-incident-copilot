import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { SYNTHETIC_OPERATOR_ID, SYNTHETIC_TENANT_ID } from '../../core/config/synthetic-tenant';
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

  it('startsNewIncidentWithConfiguredTenantAndOperator', () => {
    service.start('incident/id').subscribe();
    const request = http.expectOne(
      `/api/incidents/incident%2Fid/investigations?tenantId=${SYNTHETIC_TENANT_ID}`,
    );
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ operatorId: SYNTHETIC_OPERATOR_ID });
    request.flush({});
  });

  it('requestsTenantScopedInvestigationWorkspace', () => {
    service.get('investigation/id').subscribe();
    const request = http.expectOne(
      `/api/investigations/investigation%2Fid?tenantId=${SYNTHETIC_TENANT_ID}`,
    );
    expect(request.request.method).toBe('GET');
    request.flush({});
  });

  it('requestsEvidenceHistoryForConfiguredTenant', () => {
    service.getEvidenceHistory('investigation/id').subscribe();
    const request = http.expectOne(
      `/api/investigations/investigation%2Fid/evidence-collections?tenantId=${SYNTHETIC_TENANT_ID}`,
    );
    expect(request.request.method).toBe('GET');
    request.flush([]);
  });

  it('startsEvidenceCollectionForConfiguredTenantWithoutARequestBody', () => {
    service.collectEvidence('investigation/id').subscribe();
    const request = http.expectOne(
      `/api/investigations/investigation%2Fid/evidence-collections?tenantId=${SYNTHETIC_TENANT_ID}`,
    );
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toBeNull();
    request.flush({});
  });

  it('requestsKnowledgeHistoryForConfiguredTenant', () => {
    service.getKnowledgeHistory('investigation/id').subscribe();
    const request = http.expectOne(
      `/api/investigations/investigation%2Fid/knowledge-retrievals?tenantId=${SYNTHETIC_TENANT_ID}`,
    );
    expect(request.request.method).toBe('GET');
    request.flush([]);
  });

  it('startsKnowledgeRetrievalWithoutClientQueryParameters', () => {
    service.retrieveKnowledge('investigation/id').subscribe();
    const request = http.expectOne(
      `/api/investigations/investigation%2Fid/knowledge-retrievals?tenantId=${SYNTHETIC_TENANT_ID}`,
    );
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toBeNull();
    expect(request.request.params.keys()).toEqual(['tenantId']);
    request.flush({});
  });
});
