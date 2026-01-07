import { HttpInterceptorFn } from '@angular/common/http';

export const apiInterceptor: HttpInterceptorFn = (req, next) => {
  // Add default headers
  const modifiedReq = req.clone({
    setHeaders: {
      'Content-Type': 'application/json',
      'X-User-Id': 'workflow-builder-ui' // Can be replaced with actual user from auth
    }
  });

  return next(modifiedReq);
};
