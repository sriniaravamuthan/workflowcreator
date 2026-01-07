import { Component, Input, Output, EventEmitter, inject, OnChanges, SimpleChanges } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

import { WorkflowService } from '../../services/workflow.service';
import { TaskDefinition, NotificationType } from '../../models/workflow.model';

@Component({
  selector: 'app-notification-config',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatIconModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatCheckboxModule,
    MatExpansionModule,
    MatSnackBarModule
  ],
  template: `
    <div class="notification-config">
      <h4>
        <mat-icon>notifications</mat-icon>
        External Notifications
      </h4>
      <p class="help-text">
        Configure notifications to downstream systems when this task completes, fails, or is skipped.
      </p>

      <form [formGroup]="form" class="form-container">
        <mat-form-field appearance="outline" class="w-full">
          <mat-label>Notification Type</mat-label>
          <mat-select formControlName="notificationType">
            <mat-option value="NONE">None</mat-option>
            <mat-option value="KAFKA">Kafka Topic</mat-option>
            <mat-option value="API">REST API</mat-option>
            <mat-option value="BOTH">Both Kafka & API</mat-option>
          </mat-select>
        </mat-form-field>

        @if (showKafkaConfig()) {
          <mat-expansion-panel expanded>
            <mat-expansion-panel-header>
              <mat-panel-title>
                <mat-icon>stream</mat-icon>
                Kafka Configuration
              </mat-panel-title>
            </mat-expansion-panel-header>

            <mat-form-field appearance="outline" class="w-full">
              <mat-label>Kafka Topic</mat-label>
              <input matInput formControlName="notificationKafkaTopic"
                     placeholder="e.g., lab-orders-completed">
              <mat-hint>Topic name to publish events to</mat-hint>
            </mat-form-field>
          </mat-expansion-panel>
        }

        @if (showApiConfig()) {
          <mat-expansion-panel expanded>
            <mat-expansion-panel-header>
              <mat-panel-title>
                <mat-icon>api</mat-icon>
                API Configuration
              </mat-panel-title>
            </mat-expansion-panel-header>

            <mat-form-field appearance="outline" class="w-full">
              <mat-label>API Endpoint</mat-label>
              <input matInput formControlName="notificationApiEndpoint"
                     placeholder="https://external-system/api/webhook">
            </mat-form-field>

            <mat-form-field appearance="outline" class="w-full">
              <mat-label>HTTP Method</mat-label>
              <mat-select formControlName="notificationApiMethod">
                <mat-option value="POST">POST</mat-option>
                <mat-option value="PUT">PUT</mat-option>
              </mat-select>
            </mat-form-field>

            <mat-form-field appearance="outline" class="w-full">
              <mat-label>Custom Headers (JSON)</mat-label>
              <textarea matInput formControlName="notificationApiHeaders" rows="2"
                        placeholder='{"Authorization": "Bearer token"}'></textarea>
              <mat-hint>Optional custom headers as JSON</mat-hint>
            </mat-form-field>
          </mat-expansion-panel>
        }

        @if (showKafkaConfig() || showApiConfig()) {
          <mat-expansion-panel>
            <mat-expansion-panel-header>
              <mat-panel-title>
                <mat-icon>code</mat-icon>
                Message Template
              </mat-panel-title>
            </mat-expansion-panel-header>

            <mat-form-field appearance="outline" class="w-full">
              <mat-label>Message Template (JSON)</mat-label>
              <textarea matInput formControlName="notificationMessageTemplate" rows="8"
                        [placeholder]="templatePlaceholder"></textarea>
              <mat-hint>Use variables like $&#123;taskInstanceId&#125;, $&#123;patientMrn&#125;, etc.</mat-hint>
            </mat-form-field>

            <div class="variables-help">
              <h5>Available Variables:</h5>
              <ul>
                <li><code>$&#123;taskInstanceId&#125;</code> - Task instance UUID</li>
                <li><code>$&#123;taskName&#125;</code> - Task name</li>
                <li><code>$&#123;taskResult&#125;</code> - Task result/output</li>
                <li><code>$&#123;completedAt&#125;</code> - Completion timestamp</li>
                <li><code>$&#123;completedByUser&#125;</code> - User who completed</li>
                <li><code>$&#123;patientMrn&#125;</code> - Patient MRN</li>
                <li><code>$&#123;patientFirstName&#125;</code> - Patient first name</li>
                <li><code>$&#123;patientLastName&#125;</code> - Patient last name</li>
                <li><code>$&#123;workflowInstanceId&#125;</code> - Workflow instance UUID</li>
                <li><code>$&#123;orderId&#125;</code> - Order UUID (if applicable)</li>
                <li><code>$&#123;orderCode&#125;</code> - Order code</li>
              </ul>
            </div>
          </mat-expansion-panel>

          <!-- Trigger Options -->
          <div class="trigger-options">
            <h5>Trigger On:</h5>
            <mat-checkbox formControlName="notifyOnFailure">
              Task Failure
            </mat-checkbox>
            <mat-checkbox formControlName="notifyOnSkip">
              Task Skip
            </mat-checkbox>
            <p class="help-text small">Notifications are always sent on task completion</p>
          </div>

          <div class="action-buttons">
            <button mat-raised-button color="primary"
                    [disabled]="form.pristine"
                    (click)="saveConfig()">
              Save Configuration
            </button>
            <button mat-button color="warn" (click)="removeConfig()">
              Remove Notifications
            </button>
          </div>
        }
      </form>
    </div>
  `,
  styles: [`
    .notification-config {
      h4 {
        display: flex;
        align-items: center;
        gap: 8px;
        margin: 0 0 8px 0;
        font-size: 15px;
        font-weight: 600;

        mat-icon {
          color: #3f51b5;
        }
      }
    }

    .help-text {
      font-size: 13px;
      color: #666;
      margin-bottom: 16px;

      &.small {
        font-size: 12px;
        margin-top: 8px;
      }
    }

    .form-container {
      display: flex;
      flex-direction: column;
      gap: 12px;
    }

    .w-full {
      width: 100%;
    }

    mat-expansion-panel {
      margin-bottom: 8px;
    }

    mat-panel-title {
      display: flex;
      align-items: center;
      gap: 8px;

      mat-icon {
        font-size: 18px;
        width: 18px;
        height: 18px;
      }
    }

    .variables-help {
      margin-top: 16px;
      padding: 12px;
      background: #f5f5f5;
      border-radius: 4px;

      h5 {
        margin: 0 0 8px 0;
        font-size: 12px;
        font-weight: 600;
        color: #666;
      }

      ul {
        margin: 0;
        padding-left: 16px;
        font-size: 12px;
      }

      li {
        margin-bottom: 4px;
      }

      code {
        background: #e0e0e0;
        padding: 1px 4px;
        border-radius: 2px;
        font-size: 11px;
      }
    }

    .trigger-options {
      padding: 12px;
      background: #fafafa;
      border-radius: 4px;

      h5 {
        margin: 0 0 8px 0;
        font-size: 13px;
        font-weight: 600;
      }

      mat-checkbox {
        display: block;
        margin-bottom: 4px;
      }
    }

    .action-buttons {
      display: flex;
      flex-direction: column;
      gap: 8px;
      margin-top: 8px;
    }
  `]
})
export class NotificationConfigComponent implements OnChanges {
  @Input() task!: TaskDefinition;
  @Input() templateId!: string;
  @Output() configUpdated = new EventEmitter<void>();

  private fb = inject(FormBuilder);
  private workflowService = inject(WorkflowService);
  private snackBar = inject(MatSnackBar);

  form: FormGroup;

  templatePlaceholder = `{
  "event": "TASK_COMPLETED",
  "taskId": "\${taskInstanceId}",
  "taskName": "\${taskName}",
  "result": "\${taskResult}",
  "patient": {
    "mrn": "\${patientMrn}",
    "name": "\${patientFirstName} \${patientLastName}"
  },
  "timestamp": "\${completedAt}"
}`;

  constructor() {
    this.form = this.fb.group({
      notificationType: ['NONE'],
      notificationKafkaTopic: [''],
      notificationApiEndpoint: [''],
      notificationApiMethod: ['POST'],
      notificationApiHeaders: [''],
      notificationMessageTemplate: [''],
      notifyOnFailure: [true],
      notifyOnSkip: [false]
    });
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['task'] && this.task) {
      this.form.patchValue({
        notificationType: this.task.notificationType || 'NONE',
        notificationKafkaTopic: this.task.notificationKafkaTopic || '',
        notificationApiEndpoint: this.task.notificationApiEndpoint || '',
        notificationApiMethod: this.task.notificationApiMethod || 'POST',
        notificationApiHeaders: this.task.notificationApiHeaders || '',
        notificationMessageTemplate: this.task.notificationMessageTemplate || '',
        notifyOnFailure: this.task.notifyOnFailure ?? true,
        notifyOnSkip: this.task.notifyOnSkip ?? false
      });
      this.form.markAsPristine();
    }
  }

  showKafkaConfig(): boolean {
    const type = this.form.get('notificationType')?.value;
    return type === 'KAFKA' || type === 'BOTH';
  }

  showApiConfig(): boolean {
    const type = this.form.get('notificationType')?.value;
    return type === 'API' || type === 'BOTH';
  }

  saveConfig(): void {
    const config = this.form.value;

    this.workflowService.updateTaskNotification(this.templateId, this.task.id, config)
      .subscribe({
        next: () => {
          this.form.markAsPristine();
          this.configUpdated.emit();
          this.snackBar.open('Notification configuration saved', 'Close', { duration: 2000 });
        },
        error: () => {
          this.snackBar.open('Failed to save configuration', 'Close', { duration: 3000 });
        }
      });
  }

  removeConfig(): void {
    if (confirm('Remove notification configuration from this task?')) {
      this.workflowService.removeTaskNotification(this.templateId, this.task.id)
        .subscribe({
          next: () => {
            this.form.patchValue({ notificationType: 'NONE' });
            this.form.markAsPristine();
            this.configUpdated.emit();
            this.snackBar.open('Notification configuration removed', 'Close', { duration: 2000 });
          },
          error: () => {
            this.snackBar.open('Failed to remove configuration', 'Close', { duration: 3000 });
          }
        });
    }
  }
}
