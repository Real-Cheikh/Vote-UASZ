import { Routes } from '@angular/router';
import { HomeComponent } from './dashboard/home.component';
import { LoginComponent } from './auth/login.component';
import { DashboardComponent } from './dashboard/dashboard.component';
import { ElectionListComponent } from './elections/election-list.component';
import { CandidaciesComponent } from './candidacies/candidacies.component';
import { VotingComponent } from './voting/voting.component';
import { ReceiptCheckComponent } from './voting/receipt-check.component';
import { ResultsComponent } from './results/results.component';
import { ComplaintsComponent } from './complaints/complaints.component';
import { AdminComponent } from './administration/admin.component';
import { authGuard } from './core/guards/auth.guard';
import { commissionGuard } from './core/guards/role.guard';

export const routes: Routes = [
  { path: '', component: HomeComponent, pathMatch: 'full' },
  { path: 'login', component: LoginComponent },
  { path: 'dashboard', component: DashboardComponent, canActivate: [authGuard] },
  { path: 'elections', component: ElectionListComponent },
  { path: 'candidacies', component: CandidaciesComponent },
  { path: 'voting/:id', component: VotingComponent, canActivate: [authGuard] },
  { path: 'verify-receipt', component: ReceiptCheckComponent },
  { path: 'results', component: ResultsComponent },
  { path: 'results/:id', component: ResultsComponent },
  { path: 'complaints', component: ComplaintsComponent, canActivate: [authGuard] },
  { path: 'admin', component: AdminComponent, canActivate: [authGuard, commissionGuard] },
  { path: '**', redirectTo: '' }
];
