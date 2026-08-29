import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { SYNTHETIC_OPERATOR_ID, SYNTHETIC_TENANT_ID } from '../config/synthetic-tenant';
import {
  SYNTHETIC_OPERATOR_HEADER,
  SYNTHETIC_TENANT_HEADER,
  syntheticRequestContextInterceptor,
} from './synthetic-request-context.interceptor';
import { HttpClient } from '@angular/common/http';

describe('syntheticRequestContextInterceptor', () => {
  let client: HttpClient;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([syntheticRequestContextInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    client = TestBed.inject(HttpClient);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('attachesSyntheticTenantContextOnceToApplicationRequests', () => {
    client.get('/api/incidents').subscribe();
    const request = http.expectOne('/api/incidents');
    expect(request.request.headers.getAll(SYNTHETIC_TENANT_HEADER)).toEqual([SYNTHETIC_TENANT_ID]);
    expect(request.request.headers.has(SYNTHETIC_OPERATOR_HEADER)).toBe(false);
    request.flush([]);
  });

  it('attachesOperatorContextOnlyToInvestigationStart', () => {
    client.post('/api/incidents/incident-1/investigations', null).subscribe();
    const request = http.expectOne('/api/incidents/incident-1/investigations');
    expect(request.request.headers.get(SYNTHETIC_TENANT_HEADER)).toBe(SYNTHETIC_TENANT_ID);
    expect(request.request.headers.get(SYNTHETIC_OPERATOR_HEADER)).toBe(SYNTHETIC_OPERATOR_ID);
    request.flush({});
  });

  it('doesNotAttachSyntheticContextOutsideTheApplicationApi', () => {
    client.get('/assets/config.json').subscribe();
    const request = http.expectOne('/assets/config.json');
    expect(request.request.headers.has(SYNTHETIC_TENANT_HEADER)).toBe(false);
    request.flush({});
  });
});
