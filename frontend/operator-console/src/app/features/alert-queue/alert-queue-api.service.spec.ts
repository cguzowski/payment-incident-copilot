import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AlertQueueApiService, SYNTHETIC_TENANT_ID } from './alert-queue-api.service';

describe('AlertQueueApiService', () => {
  let service: AlertQueueApiService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [AlertQueueApiService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AlertQueueApiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('loads the queue through the tenant-scoped endpoint', () => {
    service.getQueue().subscribe((items) => expect(items).toEqual([]));

    const request = http.expectOne(`/api/tenants/${SYNTHETIC_TENANT_ID}/alert-queue`);
    expect(request.request.method).toBe('GET');
    request.flush([]);
  });
});
