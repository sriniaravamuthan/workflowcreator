import { Component } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatListModule } from '@angular/material/list';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    MatToolbarModule,
    MatButtonModule,
    MatIconModule,
    MatSidenavModule,
    MatListModule
  ],
  template: `
    <div class="app-container">
      <mat-toolbar color="primary" class="app-toolbar">
        <button mat-icon-button (click)="sidenav.toggle()">
          <mat-icon>menu</mat-icon>
        </button>
        <span class="app-title">HMIS Workflow Builder</span>
        <span class="spacer"></span>
        <button mat-icon-button>
          <mat-icon>notifications</mat-icon>
        </button>
        <button mat-icon-button>
          <mat-icon>account_circle</mat-icon>
        </button>
      </mat-toolbar>

      <mat-sidenav-container class="sidenav-container">
        <mat-sidenav #sidenav mode="side" opened class="sidenav">
          <mat-nav-list>
            <a mat-list-item routerLink="/templates" routerLinkActive="active">
              <mat-icon matListItemIcon>account_tree</mat-icon>
              <span matListItemTitle>Templates</span>
            </a>
            <a mat-list-item routerLink="/instances" routerLinkActive="active">
              <mat-icon matListItemIcon>play_circle</mat-icon>
              <span matListItemTitle>Running Workflows</span>
            </a>
            <a mat-list-item routerLink="/audit" routerLinkActive="active">
              <mat-icon matListItemIcon>history</mat-icon>
              <span matListItemTitle>Audit Trail</span>
            </a>
            <mat-divider></mat-divider>
            <a mat-list-item routerLink="/settings" routerLinkActive="active">
              <mat-icon matListItemIcon>settings</mat-icon>
              <span matListItemTitle>Settings</span>
            </a>
          </mat-nav-list>
        </mat-sidenav>

        <mat-sidenav-content class="main-content">
          <router-outlet />
        </mat-sidenav-content>
      </mat-sidenav-container>
    </div>
  `,
  styles: [`
    .app-container {
      display: flex;
      flex-direction: column;
      height: 100vh;
    }

    .app-toolbar {
      position: sticky;
      top: 0;
      z-index: 1000;
    }

    .app-title {
      margin-left: 8px;
      font-weight: 500;
    }

    .spacer {
      flex: 1;
    }

    .sidenav-container {
      flex: 1;
    }

    .sidenav {
      width: 240px;
      background: #fafafa;
    }

    .sidenav mat-nav-list a.active {
      background: rgba(63, 81, 181, 0.1);
      color: #3f51b5;
    }

    .main-content {
      background: #f5f7fa;
      padding: 24px;
    }
  `]
})
export class AppComponent {
  title = 'HMIS Workflow Builder';
}
