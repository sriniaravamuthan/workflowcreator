import { Component, Inject } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatSelectModule } from '@angular/material/select';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatIconModule } from '@angular/material/icon';

import { TaskDefinition, TaskType } from '../../models/workflow.model';

interface DialogData {
  existingTasks: TaskDefinition[];
  executionOrder: number;
}

@Component({
  selector: 'app-create-task-dialog',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatSelectModule,
    MatCheckboxModule,
    MatIconModule
  ],
  template: `
    <h2 mat-dialog-title>Add New Task</h2>

    <mat-dialog-content>
      <form [formGroup]="form" class="form-container">
        <!-- Basic Info -->
        <div class="form-section">
          <h3>Basic Information</h3>

          <mat-form-field appearance="outline" class="w-full">
            <mat-label>Task Name</mat-label>
            <input matInput formControlName="name" placeholder="e.g., Take Blood Sample">
            @if (form.get('name')?.hasError('required')) {
              <mat-error>Name is required</mat-error>
            }
          </mat-form-field>

          <mat-form-field appearance="outline" class="w-full">
            <mat-label>Description</mat-label>
            <textarea matInput formControlName="description" rows="2"
                      placeholder="Describe what this task involves"></textarea>
          </mat-form-field>

          <div class="form-row">
            <mat-form-field appearance="outline">
              <mat-label>Task Type</mat-label>
              <mat-select formControlName="taskType">
                <mat-option value="MANUAL">
                  <mat-icon>person</mat-icon> Manual
                </mat-option>
                <mat-option value="AUTOMATED">
                  <mat-icon>smart_toy</mat-icon> Automated
                </mat-option>
                <mat-option value="APPROVAL">
                  <mat-icon>thumb_up</mat-icon> Approval
                </mat-option>
                <mat-option value="NOTIFICATION">
                  <mat-icon>notifications</mat-icon> Notification
                </mat-option>
                <mat-option value="INTEGRATION">
                  <mat-icon>sync_alt</mat-icon> Integration
                </mat-option>
              </mat-select>
            </mat-form-field>

            <mat-form-field appearance="outline">
              <mat-label>Execution Order</mat-label>
              <input matInput type="number" formControlName="executionOrder" min="1">
            </mat-form-field>
          </div>
        </div>

        <!-- Assignment & SLA -->
        <div class="form-section">
          <h3>Assignment & SLA</h3>

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
            <input matInput type="number" formControlName="slaMinutes" min="0" placeholder="e.g., 60">
            <mat-hint>Leave empty for no SLA</mat-hint>
          </mat-form-field>
        </div>

        <!-- Predecessors -->
        <div class="form-section">
          <h3>Dependencies</h3>

          <mat-form-field appearance="outline" class="w-full">
            <mat-label>Predecessor Tasks</mat-label>
            <mat-select formControlName="predecessorTaskIds" multiple>
              @for (task of data.existingTasks; track task.id) {
                <mat-option [value]="task.id">
                  {{ task.name }}
                </mat-option>
              }
            </mat-select>
            <mat-hint>Tasks that must complete before this task can start</mat-hint>
          </mat-form-field>
        </div>

        <!-- Flags -->
        <div class="form-section">
          <h3>Options</h3>

          <div class="checkbox-row">
            <mat-checkbox formControlName="isOptional">
              Optional Task
            </mat-checkbox>
            <span class="hint">Can be skipped without blocking workflow</span>
          </div>

          <div class="checkbox-row">
            <mat-checkbox formControlName="isMilestone">
              Milestone
            </mat-checkbox>
            <span class="hint">Mark as a significant checkpoint in the workflow</span>
          </div>
        </div>
      </form>
    </mat-dialog-content>

    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Cancel</button>
      <button mat-raised-button color="primary"
              [disabled]="form.invalid"
              (click)="submit()">
        Add Task
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .form-container {
      display: flex;
      flex-direction: column;
      gap: 8px;
      min-width: 500px;
      padding-top: 8px;
    }

    .form-section {
      margin-bottom: 16px;

      h3 {
        font-size: 14px;
        font-weight: 600;
        color: #666;
        margin: 0 0 12px 0;
        text-transform: uppercase;
        letter-spacing: 0.5px;
      }
    }

    .form-row {
      display: flex;
      gap: 16px;

      mat-form-field {
        flex: 1;
      }
    }

    .w-full {
      width: 100%;
    }

    .checkbox-row {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-bottom: 8px;

      .hint {
        font-size: 12px;
        color: #999;
      }
    }

    mat-option mat-icon {
      margin-right: 8px;
      font-size: 18px;
      vertical-align: middle;
    }
  `]
})
export class CreateTaskDialogComponent {
  form: FormGroup;

  constructor(
    private fb: FormBuilder,
    private dialogRef: MatDialogRef<CreateTaskDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: DialogData
  ) {
    this.form = this.fb.group({
      name: ['', Validators.required],
      description: [''],
      taskType: ['MANUAL', Validators.required],
      executionOrder: [data.executionOrder, [Validators.required, Validators.min(1)]],
      defaultAssigneeRole: [''],
      slaMinutes: [null],
      predecessorTaskIds: [[]],
      isOptional: [false],
      isMilestone: [false]
    });
  }

  submit(): void {
    if (this.form.valid) {
      const value = this.form.value;
      this.dialogRef.close({
        ...value,
        slaMinutes: value.slaMinutes || null,
        predecessorTaskIds: value.predecessorTaskIds || []
      });
    }
  }
}
