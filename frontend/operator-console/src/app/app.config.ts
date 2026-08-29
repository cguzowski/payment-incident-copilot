import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideRouter } from '@angular/router';

import { apiErrorInterceptor } from './core/http/api-error.interceptor';
import { syntheticRequestContextInterceptor } from './core/http/synthetic-request-context.interceptor';
import { routes } from './app.routes';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideHttpClient(withInterceptors([syntheticRequestContextInterceptor, apiErrorInterceptor])),
    provideRouter(routes),
  ],
};
