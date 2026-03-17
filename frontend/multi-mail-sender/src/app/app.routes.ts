import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', loadComponent: () => import('./pages/main/main').then(m => m.MainComponent) },
  { path: 'dashboard', loadComponent: () => import('./pages/dashboard/dashboard').then(m => m.DashboardComponent) },
  { path: 'jobs', loadComponent: () => import('./pages/jobs/jobs').then(m => m.JobsComponent) },
  { path: 'cv', loadComponent: () => import('./pages/cv/cv').then(m => m.CvComponent) },
  { path: 'companies', loadComponent: () => import('./pages/companies/companies').then(m => m.CompaniesComponent) },
  { path: '**', redirectTo: '' }
];
