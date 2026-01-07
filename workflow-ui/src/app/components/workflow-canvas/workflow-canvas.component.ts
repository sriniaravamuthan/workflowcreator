import { Component, Input, Output, EventEmitter, inject, OnChanges, SimpleChanges, ElementRef, ViewChild } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';

import { CanvasService } from '../../services/canvas.service';
import { TaskDefinition, TaskConnection } from '../../models/workflow.model';
import { TaskNodeComponent } from '../task-node/task-node.component';

@Component({
  selector: 'app-workflow-canvas',
  standalone: true,
  imports: [MatIconModule, MatTooltipModule, TaskNodeComponent],
  template: `
    <div class="canvas-container"
         #canvasContainer
         (mousedown)="onCanvasMouseDown($event)"
         (mousemove)="onCanvasMouseMove($event)"
         (mouseup)="onCanvasMouseUp($event)"
         (wheel)="onCanvasWheel($event)">

      <!-- SVG for connection lines -->
      <svg class="connections-layer"
           [style.transform]="getTransform()">
        <defs>
          <marker id="arrowhead" markerWidth="10" markerHeight="7"
                  refX="9" refY="3.5" orient="auto">
            <polygon points="0 0, 10 3.5, 0 7" fill="#9e9e9e" />
          </marker>
        </defs>

        @for (conn of connections; track conn.fromTaskId + conn.toTaskId) {
          <path [attr.d]="getConnectionPath(conn)"
                class="connection-line"
                [class.highlighted]="isConnectionHighlighted(conn)" />
        }

        <!-- Connection preview while dragging -->
        @if (isConnecting && connectingFrom) {
          <path [attr.d]="getPreviewPath()"
                class="connection-preview" />
        }
      </svg>

      <!-- Task nodes layer -->
      <div class="tasks-layer" [style.transform]="getTransform()">
        @for (task of tasks; track task.id) {
          <app-task-node
            [task]="task"
            [position]="canvasService.getTaskPosition(task.id)"
            [isSelected]="canvasService.selectedTaskId() === task.id"
            (click)="onTaskClick(task.id, $event)"
            (dragStart)="onTaskDragStart(task.id, $event)"
            (dragMove)="onTaskDragMove($event)"
            (dragEnd)="onTaskDragEnd()"
            (connectStart)="onConnectStart(task.id)"
            (connectEnd)="onConnectEnd(task.id)"
          />
        }

        @if (tasks.length === 0) {
          <div class="empty-canvas">
            <mat-icon>add_circle_outline</mat-icon>
            <p>Add your first task to get started</p>
          </div>
        }
      </div>
    </div>
  `,
  styles: [`
    .canvas-container {
      flex: 1;
      position: relative;
      overflow: hidden;
      background-color: #fafbfc;
      background-image:
        linear-gradient(rgba(0, 0, 0, 0.03) 1px, transparent 1px),
        linear-gradient(90deg, rgba(0, 0, 0, 0.03) 1px, transparent 1px);
      background-size: 20px 20px;
      cursor: grab;

      &:active {
        cursor: grabbing;
      }
    }

    .connections-layer {
      position: absolute;
      top: 0;
      left: 0;
      width: 100%;
      height: 100%;
      pointer-events: none;
      overflow: visible;
    }

    .connection-line {
      stroke: #9e9e9e;
      stroke-width: 2;
      fill: none;
      marker-end: url(#arrowhead);
      transition: stroke 0.2s;

      &.highlighted {
        stroke: #3f51b5;
        stroke-width: 3;
      }
    }

    .connection-preview {
      stroke: #3f51b5;
      stroke-width: 2;
      stroke-dasharray: 5, 5;
      fill: none;
    }

    .tasks-layer {
      position: absolute;
      top: 0;
      left: 0;
      transform-origin: 0 0;
    }

    .empty-canvas {
      position: absolute;
      top: 50%;
      left: 50%;
      transform: translate(-50%, -50%);
      text-align: center;
      color: #999;

      mat-icon {
        font-size: 64px;
        width: 64px;
        height: 64px;
        opacity: 0.5;
      }

      p {
        margin-top: 16px;
        font-size: 16px;
      }
    }
  `]
})
export class WorkflowCanvasComponent implements OnChanges {
  @Input() tasks: TaskDefinition[] = [];
  @Output() taskSelected = new EventEmitter<string>();
  @Output() taskMoved = new EventEmitter<{ taskId: string; x: number; y: number }>();
  @Output() connectionCreated = new EventEmitter<{ fromTaskId: string; toTaskId: string }>();

  @ViewChild('canvasContainer') canvasContainer!: ElementRef<HTMLDivElement>;

  canvasService = inject(CanvasService);

  connections: TaskConnection[] = [];

  // Panning state
  isPanning = false;
  panStartX = 0;
  panStartY = 0;

  // Dragging task state
  isDraggingTask = false;
  draggingTaskId: string | null = null;
  dragStartX = 0;
  dragStartY = 0;

  // Connecting tasks state
  isConnecting = false;
  connectingFrom: string | null = null;
  mouseX = 0;
  mouseY = 0;

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['tasks']) {
      this.connections = this.canvasService.getConnections(this.tasks);
    }
  }

  getTransform(): string {
    const state = this.canvasService.canvasState();
    return `translate(${state.panX}px, ${state.panY}px) scale(${state.zoom})`;
  }

  // Task interactions
  onTaskClick(taskId: string, event: MouseEvent): void {
    event.stopPropagation();
    this.taskSelected.emit(taskId);
  }

  onTaskDragStart(taskId: string, event: { x: number; y: number }): void {
    this.isDraggingTask = true;
    this.draggingTaskId = taskId;
    this.dragStartX = event.x;
    this.dragStartY = event.y;
  }

  onTaskDragMove(event: { x: number; y: number }): void {
    if (this.isDraggingTask && this.draggingTaskId) {
      const state = this.canvasService.canvasState();
      const deltaX = (event.x - this.dragStartX) / state.zoom;
      const deltaY = (event.y - this.dragStartY) / state.zoom;

      const currentPos = this.canvasService.getTaskPosition(this.draggingTaskId);
      if (currentPos) {
        const newX = Math.max(0, currentPos.x + deltaX);
        const newY = Math.max(0, currentPos.y + deltaY);
        this.canvasService.setTaskPosition(this.draggingTaskId, newX, newY);
      }

      this.dragStartX = event.x;
      this.dragStartY = event.y;
    }
  }

  onTaskDragEnd(): void {
    if (this.isDraggingTask && this.draggingTaskId) {
      const pos = this.canvasService.getTaskPosition(this.draggingTaskId);
      if (pos) {
        this.taskMoved.emit({ taskId: this.draggingTaskId, x: pos.x, y: pos.y });
      }
    }
    this.isDraggingTask = false;
    this.draggingTaskId = null;
  }

  // Connection interactions
  onConnectStart(taskId: string): void {
    this.isConnecting = true;
    this.connectingFrom = taskId;
  }

  onConnectEnd(taskId: string): void {
    if (this.isConnecting && this.connectingFrom && this.connectingFrom !== taskId) {
      this.connectionCreated.emit({
        fromTaskId: this.connectingFrom,
        toTaskId: taskId
      });
    }
    this.isConnecting = false;
    this.connectingFrom = null;
  }

  // Canvas panning
  onCanvasMouseDown(event: MouseEvent): void {
    if (event.target === this.canvasContainer?.nativeElement) {
      this.isPanning = true;
      this.panStartX = event.clientX;
      this.panStartY = event.clientY;
      this.canvasService.clearSelection();
    }
  }

  onCanvasMouseMove(event: MouseEvent): void {
    this.mouseX = event.clientX;
    this.mouseY = event.clientY;

    if (this.isPanning) {
      const deltaX = event.clientX - this.panStartX;
      const deltaY = event.clientY - this.panStartY;
      this.canvasService.pan(deltaX, deltaY);
      this.panStartX = event.clientX;
      this.panStartY = event.clientY;
    }
  }

  onCanvasMouseUp(event: MouseEvent): void {
    this.isPanning = false;

    if (this.isConnecting) {
      this.isConnecting = false;
      this.connectingFrom = null;
    }
  }

  onCanvasWheel(event: WheelEvent): void {
    event.preventDefault();
    const delta = event.deltaY > 0 ? -0.1 : 0.1;
    this.canvasService.setZoom(this.canvasService.zoom() + delta);
  }

  // Connection path calculations
  getConnectionPath(conn: TaskConnection): string {
    const fromPos = this.canvasService.getTaskPosition(conn.fromTaskId);
    const toPos = this.canvasService.getTaskPosition(conn.toTaskId);

    if (!fromPos || !toPos) return '';

    const TASK_WIDTH = 200;
    const TASK_HEIGHT = 80;

    const startX = fromPos.x + TASK_WIDTH;
    const startY = fromPos.y + TASK_HEIGHT / 2;
    const endX = toPos.x;
    const endY = toPos.y + TASK_HEIGHT / 2;

    // Bezier curve
    const controlX = (startX + endX) / 2;

    return `M ${startX} ${startY} C ${controlX} ${startY}, ${controlX} ${endY}, ${endX} ${endY}`;
  }

  getPreviewPath(): string {
    if (!this.connectingFrom) return '';

    const fromPos = this.canvasService.getTaskPosition(this.connectingFrom);
    if (!fromPos) return '';

    const state = this.canvasService.canvasState();
    const TASK_WIDTH = 200;
    const TASK_HEIGHT = 80;

    const startX = fromPos.x + TASK_WIDTH;
    const startY = fromPos.y + TASK_HEIGHT / 2;

    // Convert mouse position to canvas coordinates
    const rect = this.canvasContainer?.nativeElement.getBoundingClientRect();
    const endX = (this.mouseX - (rect?.left || 0) - state.panX) / state.zoom;
    const endY = (this.mouseY - (rect?.top || 0) - state.panY) / state.zoom;

    const controlX = (startX + endX) / 2;

    return `M ${startX} ${startY} C ${controlX} ${startY}, ${controlX} ${endY}, ${endX} ${endY}`;
  }

  isConnectionHighlighted(conn: TaskConnection): boolean {
    const selectedId = this.canvasService.selectedTaskId();
    return selectedId === conn.fromTaskId || selectedId === conn.toTaskId;
  }
}
