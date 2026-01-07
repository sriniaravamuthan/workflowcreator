import { Component, inject, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';

import { WorkflowService } from '../../services/workflow.service';
import { CanvasService } from '../../services/canvas.service';
import { WorkflowCanvasComponent } from '../../components/workflow-canvas/workflow-canvas.component';
import { TaskConfigPanelComponent } from '../../components/task-config-panel/task-config-panel.component';
import { CreateTaskDialogComponent } from '../../components/workflow-canvas/create-task-dialog.component';
import { TaskDefinition } from '../../models/workflow.model';

@Component({
  selector: 'app-template-editor',
  standalone: true,
  imports: [
    MatToolbarModule,
    MatButtonModule,
    MatIconModule,
    MatSnackBarModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    MatDialogModule,
    WorkflowCanvasComponent,
    TaskConfigPanelComponent
  ],
  template: `
    <div class="editor-container">
      <!-- Editor Toolbar -->
      <mat-toolbar class="editor-toolbar">
        <button mat-icon-button (click)="goBack()" matTooltip="Back to templates">
          <mat-icon>arrow_back</mat-icon>
        </button>

        @if (workflowService.currentTemplate(); as template) {
          <span class="template-name">{{ template.name }}</span>
          <span class="template-status status-{{ template.status.toLowerCase() }}">
            {{ template.status }}
          </span>
        }

        <span class="spacer"></span>

        <!-- Canvas Controls -->
        <div class="canvas-controls">
          <button mat-icon-button (click)="canvasService.zoomOut()" matTooltip="Zoom out">
            <mat-icon>remove</mat-icon>
          </button>
          <span class="zoom-level">{{ (canvasService.zoom() * 100).toFixed(0) }}%</span>
          <button mat-icon-button (click)="canvasService.zoomIn()" matTooltip="Zoom in">
            <mat-icon>add</mat-icon>
          </button>
          <button mat-icon-button (click)="canvasService.resetZoom()" matTooltip="Reset view">
            <mat-icon>fit_screen</mat-icon>
          </button>
        </div>

        <div class="toolbar-divider"></div>

        <button mat-icon-button (click)="autoLayout()" matTooltip="Auto layout">
          <mat-icon>auto_fix_high</mat-icon>
        </button>

        <button mat-raised-button color="primary" (click)="addTask()">
          <mat-icon>add</mat-icon>
          Add Task
        </button>
      </mat-toolbar>

      <!-- Main Editor Area -->
      <div class="editor-content">
        @if (workflowService.loading()) {
          <div class="loading-overlay">
            <mat-spinner diameter="48"></mat-spinner>
            <p>Loading template...</p>
          </div>
        } @else if (workflowService.currentTemplate(); as template) {
          <!-- Workflow Canvas -->
          <app-workflow-canvas
            [tasks]="template.taskDefinitions"
            (taskSelected)="onTaskSelected($event)"
            (taskMoved)="onTaskMoved($event)"
            (connectionCreated)="onConnectionCreated($event)"
          />

          <!-- Task Config Panel (Sidebar) -->
          @if (canvasService.selectedTaskId(); as selectedId) {
            <app-task-config-panel
              [task]="getSelectedTask(selectedId)"
              [allTasks]="template.taskDefinitions"
              [templateId]="template.id"
              (taskUpdated)="onTaskUpdated($event)"
              (taskDeleted)="onTaskDeleted($event)"
              (close)="canvasService.clearSelection()"
            />
          }
        } @else {
          <div class="error-state">
            <mat-icon>error_outline</mat-icon>
            <h2>Template Not Found</h2>
            <button mat-raised-button color="primary" (click)="goBack()">
              Back to Templates
            </button>
          </div>
        }
      </div>
    </div>
  `,
  styles: [`
    .editor-container {
      display: flex;
      flex-direction: column;
      height: calc(100vh - 64px);
      margin: -24px;
    }

    .editor-toolbar {
      background: white;
      border-bottom: 1px solid #e0e0e0;
      padding: 0 16px;
      height: 56px;

      .template-name {
        font-weight: 600;
        margin-left: 8px;
      }

      .template-status {
        margin-left: 12px;
        padding: 4px 12px;
        border-radius: 16px;
        font-size: 12px;
        font-weight: 500;
      }

      .status-draft {
        background: #fff3e0;
        color: #e65100;
      }

      .status-active {
        background: #e8f5e9;
        color: #2e7d32;
      }
    }

    .spacer {
      flex: 1;
    }

    .canvas-controls {
      display: flex;
      align-items: center;
      gap: 4px;
      background: #f5f5f5;
      border-radius: 20px;
      padding: 4px;

      .zoom-level {
        min-width: 48px;
        text-align: center;
        font-size: 13px;
        color: #666;
      }
    }

    .toolbar-divider {
      width: 1px;
      height: 24px;
      background: #e0e0e0;
      margin: 0 16px;
    }

    .editor-content {
      flex: 1;
      display: flex;
      position: relative;
      overflow: hidden;
    }

    .loading-overlay,
    .error-state {
      position: absolute;
      inset: 0;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      background: rgba(255, 255, 255, 0.9);
      z-index: 100;

      mat-icon {
        font-size: 64px;
        width: 64px;
        height: 64px;
        color: #9e9e9e;
        margin-bottom: 16px;
      }

      p, h2 {
        color: #666;
      }
    }
  `]
})
export class TemplateEditorComponent implements OnInit, OnDestroy {
  workflowService = inject(WorkflowService);
  canvasService = inject(CanvasService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private dialog = inject(MatDialog);
  private snackBar = inject(MatSnackBar);

  ngOnInit(): void {
    const templateId = this.route.snapshot.paramMap.get('id');
    if (templateId && templateId !== 'new') {
      this.workflowService.getTemplate(templateId).subscribe({
        next: (template) => {
          // Auto-layout tasks if no positions are saved
          if (template.taskDefinitions.length > 0) {
            this.canvasService.autoLayoutTasks(template.taskDefinitions);
          }
        },
        error: () => {
          this.snackBar.open('Failed to load template', 'Close', { duration: 3000 });
          this.router.navigate(['/templates']);
        }
      });
    }
  }

  ngOnDestroy(): void {
    this.canvasService.reset();
    this.workflowService.setCurrentTemplate(null);
  }

  goBack(): void {
    this.router.navigate(['/templates']);
  }

  addTask(): void {
    const template = this.workflowService.currentTemplate();
    if (!template) return;

    const dialogRef = this.dialog.open(CreateTaskDialogComponent, {
      width: '600px',
      data: {
        existingTasks: template.taskDefinitions,
        executionOrder: template.taskDefinitions.length + 1
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.workflowService.addTask(template.id, result).subscribe({
          next: () => {
            this.snackBar.open('Task added', 'Close', { duration: 2000 });
            // Re-layout after adding
            const updatedTemplate = this.workflowService.currentTemplate();
            if (updatedTemplate) {
              this.canvasService.autoLayoutTasks(updatedTemplate.taskDefinitions);
            }
          },
          error: () => {
            this.snackBar.open('Failed to add task', 'Close', { duration: 3000 });
          }
        });
      }
    });
  }

  autoLayout(): void {
    const template = this.workflowService.currentTemplate();
    if (template) {
      this.canvasService.autoLayoutTasks(template.taskDefinitions);
    }
  }

  onTaskSelected(taskId: string): void {
    this.canvasService.selectTask(taskId);
  }

  onTaskMoved(event: { taskId: string; x: number; y: number }): void {
    this.canvasService.setTaskPosition(event.taskId, event.x, event.y);
  }

  onConnectionCreated(event: { fromTaskId: string; toTaskId: string }): void {
    const template = this.workflowService.currentTemplate();
    if (!template) return;

    const targetTask = template.taskDefinitions.find(t => t.id === event.toTaskId);
    if (targetTask) {
      const newPredecessors = [...(targetTask.predecessorTaskIds || []), event.fromTaskId];
      this.workflowService.updateTaskPredecessors(template.id, event.toTaskId, newPredecessors)
        .subscribe({
          next: () => {
            this.snackBar.open('Connection created', 'Close', { duration: 2000 });
          },
          error: () => {
            this.snackBar.open('Failed to create connection', 'Close', { duration: 3000 });
          }
        });
    }
  }

  onTaskUpdated(task: TaskDefinition): void {
    this.snackBar.open('Task updated', 'Close', { duration: 2000 });
  }

  onTaskDeleted(taskId: string): void {
    const template = this.workflowService.currentTemplate();
    if (!template) return;

    this.workflowService.deleteTask(template.id, taskId).subscribe({
      next: () => {
        this.canvasService.clearSelection();
        this.snackBar.open('Task deleted', 'Close', { duration: 2000 });
        // Re-layout after deletion
        const updatedTemplate = this.workflowService.currentTemplate();
        if (updatedTemplate) {
          this.canvasService.autoLayoutTasks(updatedTemplate.taskDefinitions);
        }
      },
      error: () => {
        this.snackBar.open('Failed to delete task', 'Close', { duration: 3000 });
      }
    });
  }

  getSelectedTask(taskId: string): TaskDefinition | undefined {
    return this.workflowService.currentTemplate()?.taskDefinitions.find(t => t.id === taskId);
  }
}
