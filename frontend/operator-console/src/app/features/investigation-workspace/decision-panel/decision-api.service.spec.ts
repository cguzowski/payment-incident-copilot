import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { DecisionApiService } from './decision-api.service';

describe('DecisionApiService', () => {
  let service: DecisionApiService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [DecisionApiService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(DecisionApiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('submitsOnlyExplicitOutcomeAndReason', () => {
    service
      .record('investigation-1', { outcome: 'APPROVED', reason: 'Reviewed evidence.' })
      .subscribe();

    const request = http.expectOne('/api/investigations/investigation-1/decisions');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ outcome: 'APPROVED', reason: 'Reviewed evidence.' });
    expect(Object.keys(request.request.body)).toEqual(['outcome', 'reason']);
    request.flush({});
  });

  it('loadsTheStoredDecisionIndependently', () => {
    service.getHistory('investigation-1').subscribe();

    const request = http.expectOne('/api/investigations/investigation-1/decisions');
    expect(request.request.method).toBe('GET');
    request.flush([]);
  });
});
