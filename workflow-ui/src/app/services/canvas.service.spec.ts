import { TestBed } from '@angular/core/testing';
import { CanvasService } from './canvas.service';
import { TaskDefinition } from '../models/workflow.model';

describe('CanvasService', () => {
  let service: CanvasService;

  const mockTasks: TaskDefinition[] = [
    {
      id: 'task-1',
      name: 'Task 1',
      taskType: 'MANUAL',
      executionOrder: 1,
      isOptional: false,
      isMilestone: false,
      predecessorTaskIds: []
    },
    {
      id: 'task-2',
      name: 'Task 2',
      taskType: 'MANUAL',
      executionOrder: 2,
      isOptional: false,
      isMilestone: false,
      predecessorTaskIds: ['task-1']
    },
    {
      id: 'task-3',
      name: 'Task 3',
      taskType: 'MANUAL',
      executionOrder: 3,
      isOptional: false,
      isMilestone: false,
      predecessorTaskIds: ['task-1']
    },
    {
      id: 'task-4',
      name: 'Task 4',
      taskType: 'MANUAL',
      executionOrder: 4,
      isOptional: false,
      isMilestone: false,
      predecessorTaskIds: ['task-2', 'task-3']
    }
  ];

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [CanvasService]
    });
    service = TestBed.inject(CanvasService);
  });

  describe('Selection', () => {
    it('should select a task', () => {
      service.selectTask('task-1');
      expect(service.selectedTaskId()).toBe('task-1');
    });

    it('should clear selection', () => {
      service.selectTask('task-1');
      service.clearSelection();
      expect(service.selectedTaskId()).toBeNull();
    });

    it('should update canvas state on selection', () => {
      service.selectTask('task-1');
      expect(service.canvasState().selectedTaskId).toBe('task-1');
    });
  });

  describe('Zoom', () => {
    it('should set zoom level', () => {
      service.setZoom(1.5);
      expect(service.zoom()).toBe(1.5);
    });

    it('should clamp zoom to minimum 0.25', () => {
      service.setZoom(0.1);
      expect(service.zoom()).toBe(0.25);
    });

    it('should clamp zoom to maximum 2', () => {
      service.setZoom(3);
      expect(service.zoom()).toBe(2);
    });

    it('should zoom in by 0.1', () => {
      service.setZoom(1);
      service.zoomIn();
      expect(service.zoom()).toBeCloseTo(1.1, 1);
    });

    it('should zoom out by 0.1', () => {
      service.setZoom(1);
      service.zoomOut();
      expect(service.zoom()).toBeCloseTo(0.9, 1);
    });

    it('should reset zoom to 1', () => {
      service.setZoom(1.5);
      service.resetZoom();
      expect(service.zoom()).toBe(1);
    });

    it('should reset pan on resetZoom', () => {
      service.pan(100, 100);
      service.resetZoom();
      expect(service.canvasState().panX).toBe(0);
      expect(service.canvasState().panY).toBe(0);
    });
  });

  describe('Pan', () => {
    it('should pan canvas', () => {
      service.pan(50, 100);
      expect(service.canvasState().panX).toBe(50);
      expect(service.canvasState().panY).toBe(100);
    });

    it('should accumulate pan values', () => {
      service.pan(50, 100);
      service.pan(25, 50);
      expect(service.canvasState().panX).toBe(75);
      expect(service.canvasState().panY).toBe(150);
    });
  });

  describe('Task Positioning', () => {
    it('should set task position', () => {
      service.setTaskPosition('task-1', 100, 200);
      const position = service.getTaskPosition('task-1');
      expect(position).toEqual({ taskId: 'task-1', x: 100, y: 200 });
    });

    it('should return undefined for unknown task', () => {
      const position = service.getTaskPosition('unknown');
      expect(position).toBeUndefined();
    });

    it('should update existing task position', () => {
      service.setTaskPosition('task-1', 100, 200);
      service.setTaskPosition('task-1', 150, 250);
      const position = service.getTaskPosition('task-1');
      expect(position).toEqual({ taskId: 'task-1', x: 150, y: 250 });
    });
  });

  describe('Auto Layout', () => {
    it('should auto layout tasks', () => {
      service.autoLayoutTasks(mockTasks);

      const pos1 = service.getTaskPosition('task-1');
      const pos2 = service.getTaskPosition('task-2');
      const pos3 = service.getTaskPosition('task-3');
      const pos4 = service.getTaskPosition('task-4');

      // All positions should be set
      expect(pos1).toBeDefined();
      expect(pos2).toBeDefined();
      expect(pos3).toBeDefined();
      expect(pos4).toBeDefined();

      // Task 1 should be at level 0 (leftmost)
      expect(pos1!.x).toBeLessThan(pos2!.x);

      // Task 2 and 3 should be at level 1 (same x position)
      expect(pos2!.x).toBe(pos3!.x);

      // Task 4 should be at level 2 (rightmost)
      expect(pos4!.x).toBeGreaterThan(pos2!.x);
    });

    it('should handle tasks without predecessors', () => {
      const singleTask: TaskDefinition[] = [{
        id: 'task-1',
        name: 'Task 1',
        taskType: 'MANUAL',
        executionOrder: 1,
        isOptional: false,
        isMilestone: false,
        predecessorTaskIds: []
      }];

      service.autoLayoutTasks(singleTask);
      const pos = service.getTaskPosition('task-1');
      expect(pos).toBeDefined();
      expect(pos!.x).toBe(50); // START_X
      expect(pos!.y).toBe(50); // START_Y
    });

    it('should handle empty task list', () => {
      service.autoLayoutTasks([]);
      expect(service.taskPositions().size).toBe(0);
    });
  });

  describe('Connections', () => {
    it('should get connections from predecessor relationships', () => {
      const connections = service.getConnections(mockTasks);

      expect(connections).toHaveLength(4);
      expect(connections).toContainEqual({ fromTaskId: 'task-1', toTaskId: 'task-2' });
      expect(connections).toContainEqual({ fromTaskId: 'task-1', toTaskId: 'task-3' });
      expect(connections).toContainEqual({ fromTaskId: 'task-2', toTaskId: 'task-4' });
      expect(connections).toContainEqual({ fromTaskId: 'task-3', toTaskId: 'task-4' });
    });

    it('should return empty connections for tasks without predecessors', () => {
      const singleTask: TaskDefinition[] = [{
        id: 'task-1',
        name: 'Task 1',
        taskType: 'MANUAL',
        executionOrder: 1,
        isOptional: false,
        isMilestone: false,
        predecessorTaskIds: []
      }];

      const connections = service.getConnections(singleTask);
      expect(connections).toHaveLength(0);
    });
  });

  describe('Dragging', () => {
    it('should set dragging state', () => {
      service.setDragging(true);
      expect(service.canvasState().isDragging).toBe(true);
    });

    it('should clear dragging state', () => {
      service.setDragging(true);
      service.setDragging(false);
      expect(service.canvasState().isDragging).toBe(false);
    });
  });

  describe('Reset', () => {
    it('should reset all state', () => {
      // Set up some state
      service.selectTask('task-1');
      service.setZoom(1.5);
      service.pan(100, 100);
      service.setTaskPosition('task-1', 200, 300);
      service.setDragging(true);

      // Reset
      service.reset();

      // Verify everything is reset
      expect(service.canvasState()).toEqual({
        zoom: 1,
        panX: 0,
        panY: 0,
        selectedTaskId: null,
        isDragging: false
      });
      expect(service.taskPositions().size).toBe(0);
    });
  });
});
