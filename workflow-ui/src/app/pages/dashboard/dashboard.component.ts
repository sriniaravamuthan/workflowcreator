import { Component, inject, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatChipsModule } from '@angular/material/chips';
import { MatMenuModule } from '@angular/material/menu';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { DatePipe } from '@angular/common';

import { WorkflowService } from '../../services/workflow.service';
import { WorkflowTemplate } from '../../models/workflow.model';
import { CreateTemplateDialogComponent } from '../../components/workflow-list/create-template-dialog.component';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    MatChipsModule,
    MatMenuModule,
    MatDialogModule,
    MatSnackBarModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    DatePipe
  ],
  template: `
    <div class="dashboard">
      <header class="dashboard-header">
        <div>
          <h1>Workflow Templates</h1>
          <p class="subtitle">Create and manage workflow templates for patient care</p>
        </div>
        <button mat-raised-button color="primary" (click)="openCreateDialog()">
          <mat-icon>add</mat-icon>
          New Template
        </button>
      </header>

      @if (workflowService.loading()) {
        <div class="loading-container">
          <mat-spinner diameter="48"></mat-spinner>
          <p>Loading templates...</p>
        </div>
      } @else if (workflowService.templates().length === 0) {
        <mat-card class="empty-state-card">
          <mat-card-content>
            <div class="empty-state">
              <mat-icon>account_tree</mat-icon>
              <h2>No Templates Yet</h2>
              <p>Create your first workflow template to get started</p>
              <button mat-raised-button color="primary" (click)="openCreateDialog()">
                <mat-icon>add</mat-icon>
                Create Template
              </button>
            </div>
          </mat-card-content>
        </mat-card>
      } @else {
        <div class="templates-grid">
          @for (template of workflowService.templates(); track template.id) {
            <mat-card class="template-card" (click)="editTemplate(template)">
              <mat-card-header>
                <mat-icon mat-card-avatar class="template-icon">account_tree</mat-icon>
                <mat-card-title>{{ template.name }}</mat-card-title>
                <mat-card-subtitle>
                  {{ template.taskDefinitions?.length || 0 }} tasks
                </mat-card-subtitle>
                <button mat-icon-button [matMenuTriggerFor]="menu"
                        (click)="$event.stopPropagation()">
                  <mat-icon>more_vert</mat-icon>
                </button>
                <mat-menu #menu="matMenu">
                  <button mat-menu-item (click)="editTemplate(template)">
                    <mat-icon>edit</mat-icon>
                    <span>Edit</span>
                  </button>
                  <button mat-menu-item (click)="cloneTemplate(template)">
                    <mat-icon>content_copy</mat-icon>
                    <span>Clone</span>
                  </button>
                  @if (template.status === 'DRAFT') {
                    <button mat-menu-item (click)="activateTemplate(template)">
                      <mat-icon>check_circle</mat-icon>
                      <span>Activate</span>
                    </button>
                  }
                  <button mat-menu-item class="delete-item" (click)="deleteTemplate(template)">
                    <mat-icon>delete</mat-icon>
                    <span>Delete</span>
                  </button>
                </mat-menu>
              </mat-card-header>

              <mat-card-content>
                <p class="template-description">
                  {{ template.description || 'No description provided' }}
                </p>

                <div class="template-meta">
                  <mat-chip-set>
                    <mat-chip [class]="'status-' + template.status.toLowerCase()">
                      {{ template.status }}
                    </mat-chip>
                    @if (template.category) {
                      <mat-chip>{{ template.category }}</mat-chip>
                    }
                  </mat-chip-set>
                </div>
              </mat-card-content>

              <mat-card-footer>
                <small class="text-muted">
                  Updated {{ template.updatedAt | date:'medium' }}
                </small>
              </mat-card-footer>
            </mat-card>
          }
        </div>
      }
    </div>
  `,
  styles: [`
    .dashboard {
      max-width: 1400px;
      margin: 0 auto;
    }

    .dashboard-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      margin-bottom: 24px;

      h1 {
        margin: 0 0 4px 0;
        font-size: 28px;
        font-weight: 600;
      }

      .subtitle {
        margin: 0;
        color: #666;
      }
    }

    .loading-container {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: 64px;
      color: #666;
    }

    .empty-state-card {
      max-width: 500px;
      margin: 48px auto;
    }

    .empty-state {
      text-align: center;
      padding: 48px;

      mat-icon {
        font-size: 64px;
        width: 64px;
        height: 64px;
        color: #9e9e9e;
        margin-bottom: 16px;
      }

      h2 {
        margin: 0 0 8px 0;
        color: #333;
      }

      p {
        margin: 0 0 24px 0;
        color: #666;
      }
    }

    .templates-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
      gap: 24px;
    }

    .template-card {
      cursor: pointer;
      transition: transform 0.2s, box-shadow 0.2s;

      &:hover {
        transform: translateY(-2px);
        box-shadow: 0 4px 20px rgba(0, 0, 0, 0.12);
      }

      mat-card-header {
        position: relative;

        button[mat-icon-button] {
          position: absolute;
          right: 8px;
          top: 8px;
        }
      }

      .template-icon {
        background: #e8eaf6;
        color: #3f51b5;
        border-radius: 8px;
      }

      .template-description {
        color: #666;
        font-size: 14px;
        margin: 16px 0;
        display: -webkit-box;
        -webkit-line-clamp: 2;
        -webkit-box-orient: vertical;
        overflow: hidden;
      }

      .template-meta {
        margin-top: 12px;
      }

      mat-card-footer {
        padding: 8px 16px;
        border-top: 1px solid #e0e0e0;
      }
    }

    .status-draft {
      background-color: #fff3e0 !important;
      color: #e65100 !important;
    }

    .status-active {
      background-color: #e8f5e9 !important;
      color: #2e7d32 !important;
    }

    .status-deprecated {
      background-color: #fce4ec !important;
      color: #c2185b !important;
    }

    .delete-item {
      color: #f44336;
    }

    .text-muted {
      color: #999;
    }
  `]
})
export class DashboardComponent implements OnInit {
  workflowService = inject(WorkflowService);
  private router = inject(Router);
  private dialog = inject(MatDialog);
  private snackBar = inject(MatSnackBar);

  ngOnInit(): void {
    this.workflowService.loadTemplates().subscribe();
  }

  openCreateDialog(): void {
    const dialogRef = this.dialog.open(CreateTemplateDialogComponent, {
      width: '500px'
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.workflowService.createTemplate(result).subscribe({
          next: (template) => {
            this.snackBar.open('Template created successfully', 'Close', { duration: 3000 });
            this.router.navigate(['/templates', template.id]);
          },
          error: () => {
            this.snackBar.open('Failed to create template', 'Close', { duration: 3000 });
          }
        });
      }
    });
  }

  editTemplate(template: WorkflowTemplate): void {
    this.router.navigate(['/templates', template.id]);
  }

  cloneTemplate(template: WorkflowTemplate): void {
    const newName = `${template.name} (Copy)`;
    this.workflowService.cloneTemplate(template.id, newName).subscribe({
      next: () => {
        this.snackBar.open('Template cloned successfully', 'Close', { duration: 3000 });
      },
      error: () => {
        this.snackBar.open('Failed to clone template', 'Close', { duration: 3000 });
      }
    });
  }

  activateTemplate(template: WorkflowTemplate): void {
    this.workflowService.activateTemplate(template.id).subscribe({
      next: () => {
        this.snackBar.open('Template activated', 'Close', { duration: 3000 });
      },
      error: () => {
        this.snackBar.open('Failed to activate template', 'Close', { duration: 3000 });
      }
    });
  }

  deleteTemplate(template: WorkflowTemplate): void {
    if (confirm(`Are you sure you want to delete "${template.name}"?`)) {
      this.workflowService.deleteTemplate(template.id).subscribe({
        next: () => {
          this.snackBar.open('Template deleted', 'Close', { duration: 3000 });
        },
        error: () => {
          this.snackBar.open('Failed to delete template', 'Close', { duration: 3000 });
        }
      });
    }
  }
}
