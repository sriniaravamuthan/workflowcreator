import { Component, Input, Output, EventEmitter, inject, OnChanges, SimpleChanges } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatTabsModule } from '@angular/material/tabs';
import { MatDividerModule } from '@angular/material/divider';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatChipsModule } from '@angular/material/chips';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

import { WorkflowService } from '../../services/workflow.service';
import { TaskDefinition } from '../../models/workflow.model';
import { NotificationConfigComponent } from '../notification-config/notification-config.component';

@Component({
  selector: 'app-task-config-panel',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatIconModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatCheckboxModule,
    MatTabsModule,
    MatDividerModule,
    MatExpansionModule,
    MatChipsModule,
    MatSnackBarModule,
    NotificationConfigComponent
  ],
  template: `
    <div class="config-panel">
      <!-- Panel Header -->
      <div class="panel-header">
        <h3>Task Configuration</h3>
        <button mat-icon-button (click)="close.emit()">
          <mat-icon>close</mat-icon>
        </button>
      </div>

      @if (task) {
        <mat-tab-group>
          <!-- General Tab -->
          <mat-tab label="General">
            <div class="tab-content">
              <form [formGroup]="form" class="form-container">
                <mat-form-field appearance="outline" class="w-full">
                  <mat-label>Task Name</mat-label>
                  <input matInput formControlName="name">
                </mat-form-field>

                <mat-form-field appearance="outline" class="w-full">
                  <mat-label>Description</mat-label>
                  <textarea matInput formControlName="description" rows="3"></textarea>
                </mat-form-field>

                <mat-form-field appearance="outline" class="w-full">
                  <mat-label>Task Type</mat-label>
                  <mat-select formControlName="taskType">
                    <mat-option value="MANUAL">Manual</mat-option>
                    <mat-option value="AUTOMATED">Automated</mat-option>
                    <mat-option value="APPROVAL">Approval</mat-option>
                    <mat-option value="NOTIFICATION">Notification</mat-option>
                    <mat-option value="INTEGRATION">Integration</mat-option>
                  </mat-select>
                </mat-form-field>

                <mat-form-field appearance="outline" class="w-full">
                  <mat-label>Default Assignee Role</mat-label>
                  <mat-select formControlName="defaultAssigneeRole">
                    <mat-option value="">None</mat-option>
                    <mat-option value="PHYSICIAN">Physician</mat-option>
                    <mat-option value="NURSE">Nurse</mat-option>
                    <mat-option value="LAB_TECHNICIAN">Lab Technician</mat-option>
                    <mat-option value="PHARMACIST">Pharmacist</mat-option>
                    <mat-option value="RADIOLOGIST">Radiologist</mat-option>
                    <mat-option value="ADMIN">Admin</mat-option>
                  </mat-select>
                </mat-form-field>

                <mat-form-field appearance="outline" class="w-full">
                  <mat-label>SLA (minutes)</mat-label>
                  <input matInput type="number" formControlName="slaMinutes" min="0">
                </mat-form-field>

                <mat-form-field appearance="outline" class="w-full">
                  <mat-label>Execution Order</mat-label>
                  <input matInput type="number" formControlName="executionOrder" min="1">
                </mat-form-field>

                <div class="checkbox-group">
                  <mat-checkbox formControlName="isOptional">Optional Task</mat-checkbox>
                  <mat-checkbox formControlName="isMilestone">Milestone</mat-checkbox>
                </div>

                <button mat-raised-button color="primary"
                        [disabled]="form.invalid || form.pristine"
                        (click)="saveChanges()">
                  Save Changes
                </button>
              </form>
            </div>
          </mat-tab>

          <!-- Dependencies Tab -->
          <mat-tab label="Dependencies">
            <div class="tab-content">
              <h4>Predecessor Tasks</h4>
              <p class="help-text">
                Select tasks that must complete before this task can start.
              </p>

              <mat-form-field appearance="outline" class="w-full">
                <mat-label>Predecessors</mat-label>
                <mat-select [value]="task.predecessorTaskIds || []"
                            multiple
                            (selectionChange)="updatePredecessors($event.value)">
                  @for (t of availablePredecessors; track t.id) {
                    <mat-option [value]="t.id">{{ t.name }}</mat-option>
                  }
                </mat-select>
              </mat-form-field>

              @if (task.predecessorTaskIds?.length) {
                <div class="predecessor-list">
                  <h5>Current Predecessors:</h5>
                  <mat-chip-set>
                    @for (predId of task.predecessorTaskIds; track predId) {
                      <mat-chip [removable]="true"
                                (removed)="removePredecessor(predId)">
                        {{ getPredecessorName(predId) }}
                        <mat-icon matChipRemove>cancel</mat-icon>
                      </mat-chip>
                    }
                  </mat-chip-set>
                </div>
              } @else {
                <p class="no-predecessors">No predecessors configured. This task will start immediately.</p>
              }
            </div>
          </mat-tab>

          <!-- Notifications Tab -->
          <mat-tab label="Notifications">
            <div class="tab-content">
              <app-notification-config
                [task]="task"
                [templateId]="templateId"
                (configUpdated)="onNotificationUpdated()"
              />
            </div>
          </mat-tab>
        </mat-tab-group>

        <!-- Delete Action -->
        <div class="panel-footer">
          <mat-divider></mat-divider>
          <button mat-button color="warn" (click)="deleteTask()">
            <mat-icon>delete</mat-icon>
            Delete Task
          </button>
        </div>
      }
    </div>
  `,
  styles: [`
    .config-panel {
      width: 360px;
      background: white;
      border-left: 1px solid #e0e0e0;
      display: flex;
      flex-direction: column;
      height: 100%;
    }

    .panel-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 16px;
      border-bottom: 1px solid #e0e0e0;

      h3 {
        margin: 0;
        font-size: 16px;
        font-weight: 600;
      }
    }

    .tab-content {
      padding: 16px;
      overflow-y: auto;
    }

    .form-container {
      display: flex;
      flex-direction: column;
      gap: 8px;
    }

    .w-full {
      width: 100%;
    }

    .checkbox-group {
      display: flex;
      flex-direction: column;
      gap: 8px;
      margin: 8px 0 16px;
    }

    .help-text {
      font-size: 13px;
      color: #666;
      margin-bottom: 16px;
    }

    .predecessor-list {
      margin-top: 16px;

      h5 {
        font-size: 13px;
        font-weight: 500;
        color: #666;
        margin: 0 0 8px 0;
      }
    }

    .no-predecessors {
      color: #999;
      font-style: italic;
      font-size: 13px;
    }

    .panel-footer {
      margin-top: auto;
      padding: 16px;

      mat-divider {
        margin-bottom: 16px;
      }

      button {
        width: 100%;
      }
    }
  `]
})
export class TaskConfigPanelComponent implements OnChanges {
  @Input() task: TaskDefinition | undefined;
  @Input() allTasks: TaskDefinition[] = [];
  @Input() templateId!: string;

  @Output() taskUpdated = new EventEmitter<TaskDefinition>();
  @Output() taskDeleted = new EventEmitter<string>();
  @Output() close = new EventEmitter<void>();

  private fb = inject(FormBuilder);
  private workflowService = inject(WorkflowService);
  private snackBar = inject(MatSnackBar);

  form: FormGroup;

  constructor() {
    this.form = this.fb.group({
      name: ['', Validators.required],
      description: [''],
      taskType: ['MANUAL'],
      defaultAssigneeRole: [''],
      slaMinutes: [null],
      executionOrder: [1, [Validators.required, Validators.min(1)]],
      isOptional: [false],
      isMilestone: [false]
    });
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['task'] && this.task) {
      this.form.patchValue({
        name: this.task.name,
        description: this.task.description || '',
        taskType: this.task.taskType,
        defaultAssigneeRole: this.task.defaultAssigneeRole || '',
        slaMinutes: this.task.slaMinutes,
        executionOrder: this.task.executionOrder,
        isOptional: this.task.isOptional,
        isMilestone: this.task.isMilestone
      });
      this.form.markAsPristine();
    }
  }

  get availablePredecessors(): TaskDefinition[] {
    if (!this.task) return [];
    // Exclude current task from predecessors
    return this.allTasks.filter(t => t.id !== this.task!.id);
  }

  getPredecessorName(predId: string): string {
    const pred = this.allTasks.find(t => t.id === predId);
    return pred?.name || 'Unknown';
  }

  saveChanges(): void {
    if (this.form.valid && this.task) {
      const updates = {
        ...this.form.value,
        slaMinutes: this.form.value.slaMinutes || null
      };

      this.workflowService.updateTask(this.templateId, this.task.id, updates).subscribe({
        next: (updated) => {
          this.taskUpdated.emit(updated);
          this.form.markAsPristine();
          this.snackBar.open('Task updated', 'Close', { duration: 2000 });
        },
        error: () => {
          this.snackBar.open('Failed to update task', 'Close', { duration: 3000 });
        }
      });
    }
  }

  updatePredecessors(predecessorIds: string[]): void {
    if (!this.task) return;

    this.workflowService.updateTaskPredecessors(this.templateId, this.task.id, predecessorIds)
      .subscribe({
        next: (updated) => {
          this.taskUpdated.emit(updated);
          this.snackBar.open('Predecessors updated', 'Close', { duration: 2000 });
        },
        error: () => {
          this.snackBar.open('Failed to update predecessors', 'Close', { duration: 3000 });
        }
      });
  }

  removePredecessor(predId: string): void {
    if (!this.task) return;

    const newPredecessors = (this.task.predecessorTaskIds || []).filter(id => id !== predId);
    this.updatePredecessors(newPredecessors);
  }

  onNotificationUpdated(): void {
    // Refresh task data
    if (this.task) {
      this.workflowService.getTemplate(this.templateId).subscribe();
    }
  }

  deleteTask(): void {
    if (this.task && confirm(`Are you sure you want to delete "${this.task.name}"?`)) {
      this.taskDeleted.emit(this.task.id);
    }
  }
}
