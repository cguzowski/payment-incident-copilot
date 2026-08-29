import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ReportApiService } from './report-api.service';

describe('ReportApiService', () => {
  let service: ReportApiService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(ReportApiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('loadsHistoryFromTheInvestigationReportResource', () => {
    service.getHistory('investigation/1').subscribe();
    const request = http.expectOne('/api/investigations/investigation%2F1/reports');
    expect(request.request.method).toBe('GET');
    request.flush([]);
  });

  it('generatesWithNoClientPromptModelSchemaOrSources', () => {
    service.generate('investigation-1').subscribe();
    const request = http.expectOne('/api/investigations/investigation-1/reports');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toBeNull();
    request.flush({});
  });
});
