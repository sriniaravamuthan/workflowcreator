import { Injectable, signal, computed } from '@angular/core';
import { TaskDefinition, CanvasState, TaskPosition, TaskConnection } from '../models/workflow.model';

@Injectable({
  providedIn: 'root'
})
export class CanvasService {
  // Canvas state
  private _canvasState = signal<CanvasState>({
    zoom: 1,
    panX: 0,
    panY: 0,
    selectedTaskId: null,
    isDragging: false
  });

  // Task positions (for visual layout)
  private _taskPositions = signal<Map<string, TaskPosition>>(new Map());

  // Public readonly signals
  readonly canvasState = this._canvasState.asReadonly();
  readonly taskPositions = this._taskPositions.asReadonly();

  // Computed values
  readonly selectedTaskId = computed(() => this._canvasState().selectedTaskId);
  readonly zoom = computed(() => this._canvasState().zoom);

  // ==================== Selection ====================

  selectTask(taskId: string | null): void {
    this._canvasState.update(state => ({
      ...state,
      selectedTaskId: taskId
    }));
  }

  clearSelection(): void {
    this.selectTask(null);
  }

  // ==================== Zoom & Pan ====================

  setZoom(zoom: number): void {
    const clampedZoom = Math.max(0.25, Math.min(2, zoom));
    this._canvasState.update(state => ({
      ...state,
      zoom: clampedZoom
    }));
  }

  zoomIn(): void {
    this.setZoom(this._canvasState().zoom + 0.1);
  }

  zoomOut(): void {
    this.setZoom(this._canvasState().zoom - 0.1);
  }

  resetZoom(): void {
    this._canvasState.update(state => ({
      ...state,
      zoom: 1,
      panX: 0,
      panY: 0
    }));
  }

  pan(deltaX: number, deltaY: number): void {
    this._canvasState.update(state => ({
      ...state,
      panX: state.panX + deltaX,
      panY: state.panY + deltaY
    }));
  }

  // ==================== Task Positioning ====================

  setTaskPosition(taskId: string, x: number, y: number): void {
    this._taskPositions.update(positions => {
      const newPositions = new Map(positions);
      newPositions.set(taskId, { taskId, x, y });
      return newPositions;
    });
  }

  getTaskPosition(taskId: string): TaskPosition | undefined {
    return this._taskPositions().get(taskId);
  }

  /**
   * Auto-layout tasks in a grid pattern based on execution order and dependencies
   */
  autoLayoutTasks(tasks: TaskDefinition[]): void {
    const TASK_WIDTH = 220;
    const TASK_HEIGHT = 100;
    const HORIZONTAL_GAP = 80;
    const VERTICAL_GAP = 60;
    const START_X = 50;
    const START_Y = 50;

    // Group tasks by their "level" (based on predecessors)
    const levels = this.calculateTaskLevels(tasks);

    // Position tasks by level
    const newPositions = new Map<string, TaskPosition>();

    levels.forEach((taskIds, level) => {
      taskIds.forEach((taskId, index) => {
        const x = START_X + level * (TASK_WIDTH + HORIZONTAL_GAP);
        const y = START_Y + index * (TASK_HEIGHT + VERTICAL_GAP);
        newPositions.set(taskId, { taskId, x, y });
      });
    });

    this._taskPositions.set(newPositions);
  }

  /**
   * Calculate task levels based on predecessor relationships
   */
  private calculateTaskLevels(tasks: TaskDefinition[]): Map<number, string[]> {
    const levels = new Map<number, string[]>();
    const taskLevelMap = new Map<string, number>();

    // First, find all tasks without predecessors (level 0)
    const tasksById = new Map(tasks.map(t => [t.id, t]));

    const calculateLevel = (taskId: string, visited: Set<string> = new Set()): number => {
      if (taskLevelMap.has(taskId)) {
        return taskLevelMap.get(taskId)!;
      }

      if (visited.has(taskId)) {
        // Cycle detected, return 0
        return 0;
      }

      visited.add(taskId);

      const task = tasksById.get(taskId);
      if (!task || !task.predecessorTaskIds || task.predecessorTaskIds.length === 0) {
        taskLevelMap.set(taskId, 0);
        return 0;
      }

      const maxPredecessorLevel = Math.max(
        ...task.predecessorTaskIds.map(predId => calculateLevel(predId, new Set(visited)))
      );

      const level = maxPredecessorLevel + 1;
      taskLevelMap.set(taskId, level);
      return level;
    };

    // Calculate levels for all tasks
    tasks.forEach(task => calculateLevel(task.id));

    // Group tasks by level
    taskLevelMap.forEach((level, taskId) => {
      if (!levels.has(level)) {
        levels.set(level, []);
      }
      levels.get(level)!.push(taskId);
    });

    return levels;
  }

  /**
   * Get connections between tasks for rendering arrows
   */
  getConnections(tasks: TaskDefinition[]): TaskConnection[] {
    const connections: TaskConnection[] = [];

    tasks.forEach(task => {
      if (task.predecessorTaskIds) {
        task.predecessorTaskIds.forEach(predId => {
          connections.push({
            fromTaskId: predId,
            toTaskId: task.id
          });
        });
      }
    });

    return connections;
  }

  // ==================== Dragging ====================

  setDragging(isDragging: boolean): void {
    this._canvasState.update(state => ({
      ...state,
      isDragging
    }));
  }

  // ==================== Reset ====================

  reset(): void {
    this._canvasState.set({
      zoom: 1,
      panX: 0,
      panY: 0,
      selectedTaskId: null,
      isDragging: false
    });
    this._taskPositions.set(new Map());
  }
}
