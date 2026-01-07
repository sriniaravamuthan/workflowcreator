import { Component, Input, Output, EventEmitter, HostListener } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';

import { TaskDefinition, TaskPosition } from '../../models/workflow.model';

@Component({
  selector: 'app-task-node',
  standalone: true,
  imports: [MatIconModule, MatTooltipModule],
  template: `
    <div class="task-node"
         [class.selected]="isSelected"
         [class.optional]="task.isOptional"
         [class.milestone]="task.isMilestone"
         [style.left.px]="position?.x ?? 0"
         [style.top.px]="position?.y ?? 0"
         (mousedown)="onMouseDown($event)">

      <!-- Connection handle (output) -->
      <div class="connection-handle output"
           matTooltip="Drag to connect"
           (mousedown)="onConnectHandleDown($event)">
        <mat-icon>arrow_forward</mat-icon>
      </div>

      <!-- Connection handle (input) -->
      <div class="connection-handle input"
           (mouseup)="onConnectHandleUp()">
      </div>

      <!-- Task header -->
      <div class="task-header">
        <div class="task-icon" [class]="'type-' + task.taskType.toLowerCase()">
          <mat-icon>{{ getTaskIcon() }}</mat-icon>
        </div>
        <div class="task-title" [matTooltip]="task.name">
          {{ task.name }}
        </div>
      </div>

      <!-- Task meta info -->
      <div class="task-meta">
        @if (task.slaMinutes) {
          <span class="task-badge" matTooltip="SLA">
            <mat-icon>schedule</mat-icon>
            {{ formatSLA(task.slaMinutes) }}
          </span>
        }
        @if (task.isOptional) {
          <span class="task-badge optional" matTooltip="Optional task">
            Optional
          </span>
        }
        @if (task.isMilestone) {
          <span class="task-badge milestone" matTooltip="Milestone">
            <mat-icon>flag</mat-icon>
          </span>
        }
        @if (task.notificationType && task.notificationType !== 'NONE') {
          <span class="task-badge notification" matTooltip="Has notification">
            <mat-icon>notifications</mat-icon>
          </span>
        }
      </div>

      <!-- Predecessor indicator -->
      @if (task.predecessorTaskIds?.length) {
        <div class="predecessor-count" matTooltip="Has {{ task.predecessorTaskIds.length }} predecessor(s)">
          {{ task.predecessorTaskIds.length }}
        </div>
      }
    </div>
  `,
  styles: [`
    .task-node {
      position: absolute;
      width: 200px;
      min-height: 80px;
      background: white;
      border: 2px solid #e1e5eb;
      border-radius: 8px;
      padding: 12px;
      cursor: move;
      transition: box-shadow 0.2s, border-color 0.2s;
      user-select: none;

      &:hover {
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
        border-color: #3f51b5;

        .connection-handle {
          opacity: 1;
        }
      }

      &.selected {
        border-color: #3f51b5;
        box-shadow: 0 0 0 3px rgba(63, 81, 181, 0.2);
      }

      &.optional {
        border-style: dashed;
      }

      &.milestone {
        border-width: 3px;
        border-color: #ff9800;
      }
    }

    .connection-handle {
      position: absolute;
      width: 20px;
      height: 20px;
      border-radius: 50%;
      background: #3f51b5;
      color: white;
      display: flex;
      align-items: center;
      justify-content: center;
      cursor: crosshair;
      opacity: 0;
      transition: opacity 0.2s;

      mat-icon {
        font-size: 14px;
        width: 14px;
        height: 14px;
      }

      &.output {
        right: -10px;
        top: 50%;
        transform: translateY(-50%);
      }

      &.input {
        left: -10px;
        top: 50%;
        transform: translateY(-50%);
        background: #9e9e9e;
      }
    }

    .task-header {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 8px;
    }

    .task-icon {
      width: 28px;
      height: 28px;
      border-radius: 6px;
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;

      mat-icon {
        font-size: 16px;
        width: 16px;
        height: 16px;
      }

      &.type-manual {
        background: #e3f2fd;
        color: #1976d2;
      }

      &.type-automated {
        background: #e8f5e9;
        color: #388e3c;
      }

      &.type-approval {
        background: #fff3e0;
        color: #f57c00;
      }

      &.type-notification {
        background: #f3e5f5;
        color: #7b1fa2;
      }

      &.type-integration {
        background: #e0f7fa;
        color: #0097a7;
      }
    }

    .task-title {
      font-weight: 600;
      font-size: 14px;
      flex: 1;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      color: #333;
    }

    .task-meta {
      display: flex;
      gap: 6px;
      flex-wrap: wrap;
    }

    .task-badge {
      display: inline-flex;
      align-items: center;
      gap: 4px;
      padding: 2px 6px;
      border-radius: 4px;
      font-size: 11px;
      background: #f5f5f5;
      color: #666;

      mat-icon {
        font-size: 12px;
        width: 12px;
        height: 12px;
      }

      &.optional {
        background: #fff3e0;
        color: #e65100;
      }

      &.milestone {
        background: #fff8e1;
        color: #ff8f00;
      }

      &.notification {
        background: #e8eaf6;
        color: #3f51b5;
      }
    }

    .predecessor-count {
      position: absolute;
      top: -8px;
      left: -8px;
      width: 20px;
      height: 20px;
      border-radius: 50%;
      background: #9e9e9e;
      color: white;
      font-size: 11px;
      font-weight: 600;
      display: flex;
      align-items: center;
      justify-content: center;
    }
  `]
})
export class TaskNodeComponent {
  @Input() task!: TaskDefinition;
  @Input() position?: TaskPosition;
  @Input() isSelected = false;

  @Output() dragStart = new EventEmitter<{ x: number; y: number }>();
  @Output() dragMove = new EventEmitter<{ x: number; y: number }>();
  @Output() dragEnd = new EventEmitter<void>();
  @Output() connectStart = new EventEmitter<void>();
  @Output() connectEnd = new EventEmitter<void>();

  private isDragging = false;

  onMouseDown(event: MouseEvent): void {
    if ((event.target as HTMLElement).closest('.connection-handle')) {
      return; // Don't start drag if clicking connection handle
    }

    event.stopPropagation();
    this.isDragging = true;
    this.dragStart.emit({ x: event.clientX, y: event.clientY });
  }

  @HostListener('document:mousemove', ['$event'])
  onMouseMove(event: MouseEvent): void {
    if (this.isDragging) {
      this.dragMove.emit({ x: event.clientX, y: event.clientY });
    }
  }

  @HostListener('document:mouseup')
  onMouseUp(): void {
    if (this.isDragging) {
      this.isDragging = false;
      this.dragEnd.emit();
    }
  }

  onConnectHandleDown(event: MouseEvent): void {
    event.stopPropagation();
    this.connectStart.emit();
  }

  onConnectHandleUp(): void {
    this.connectEnd.emit();
  }

  getTaskIcon(): string {
    switch (this.task.taskType) {
      case 'MANUAL': return 'person';
      case 'AUTOMATED': return 'smart_toy';
      case 'APPROVAL': return 'thumb_up';
      case 'NOTIFICATION': return 'notifications';
      case 'INTEGRATION': return 'sync_alt';
      default: return 'task';
    }
  }

  formatSLA(minutes: number): string {
    if (minutes < 60) {
      return `${minutes}m`;
    }
    const hours = Math.floor(minutes / 60);
    const mins = minutes % 60;
    return mins > 0 ? `${hours}h ${mins}m` : `${hours}h`;
  }
}
