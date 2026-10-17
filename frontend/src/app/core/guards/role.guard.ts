import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const commissionGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isCommission()) {
    return true;
  }

  router.navigate(['/']);
  return false;
};

export const superAdminGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isSuperAdmin()) {
    return true;
  }

  router.navigate(['/']);
  return false;
};
