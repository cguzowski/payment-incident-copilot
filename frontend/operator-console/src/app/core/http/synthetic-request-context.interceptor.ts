import { HttpInterceptorFn } from '@angular/common/http';
import { SYNTHETIC_OPERATOR_ID, SYNTHETIC_TENANT_ID } from '../config/synthetic-tenant';

export const SYNTHETIC_TENANT_HEADER = 'X-Synthetic-Tenant-Id';
export const SYNTHETIC_OPERATOR_HEADER = 'X-Synthetic-Operator-Id';

export const syntheticRequestContextInterceptor: HttpInterceptorFn = (request, next) => {
  if (!request.url.startsWith('/api/')) {
    return next(request);
  }

  let headers = request.headers.set(SYNTHETIC_TENANT_HEADER, SYNTHETIC_TENANT_ID);
  if (isInvestigationStart(request.method, request.url)) {
    headers = headers.set(SYNTHETIC_OPERATOR_HEADER, SYNTHETIC_OPERATOR_ID);
  }
  return next(request.clone({ headers }));
};

function isInvestigationStart(method: string, url: string): boolean {
  return method === 'POST' && /^\/api\/incidents\/[^/]+\/investigations$/.test(url);
}
