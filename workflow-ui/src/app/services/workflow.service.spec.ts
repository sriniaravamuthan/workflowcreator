import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { WorkflowService } from './workflow.service';
import { WorkflowTemplate, TaskDefinition, CreateTemplateRequest } from '../models/workflow.model';

describe('WorkflowService', () => {
  let service: WorkflowService;
  let httpMock: HttpTestingController;

  const mockTemplate: WorkflowTemplate = {
    id: '123',
    name: 'Test Template',
    description: 'Test description',
    version: '1.0',
    status: 'DRAFT',
    category: 'ADMISSION',
    taskDefinitions: [],
    createdAt: '2024-01-01T00:00:00',
    updatedAt: '2024-01-01T00:00:00'
  };

  const mockTask: TaskDefinition = {
    id: 'task-1',
    name: 'Blood Test',
    taskType: 'MANUAL',
    executionOrder: 1,
    isOptional: false,
    isMilestone: false,
    predecessorTaskIds: []
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [WorkflowService]
    });

    service = TestBed.inject(WorkflowService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('Template Operations', () => {
    it('should load templates', () => {
      const mockTemplates = [mockTemplate];

      service.loadTemplates().subscribe(templates => {
        expect(templates).toEqual(mockTemplates);
        expect(service.templates()).toEqual(mockTemplates);
      });

      const req = httpMock.expectOne('/api/workflow-templates');
      expect(req.request.method).toBe('GET');
      req.flush(mockTemplates);
    });

    it('should get template by id', () => {
      service.getTemplate('123').subscribe(template => {
        expect(template).toEqual(mockTemplate);
        expect(service.currentTemplate()).toEqual(mockTemplate);
      });

      const req = httpMock.expectOne('/api/workflow-templates/123');
      expect(req.request.method).toBe('GET');
      req.flush(mockTemplate);
    });

    it('should create template', () => {
      const request: CreateTemplateRequest = {
        name: 'New Template',
        description: 'New description',
        category: 'SURGICAL'
      };

      service.createTemplate(request).subscribe(template => {
        expect(template).toEqual(mockTemplate);
      });

      const req = httpMock.expectOne('/api/workflow-templates');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(request);
      req.flush(mockTemplate);
    });

    it('should update template', () => {
      const updates = { name: 'Updated Name' };

      service.updateTemplate('123', updates).subscribe(template => {
        expect(template).toEqual(mockTemplate);
      });

      const req = httpMock.expectOne('/api/workflow-templates/123');
      expect(req.request.method).toBe('PUT');
      expect(req.request.body).toEqual(updates);
      req.flush(mockTemplate);
    });

    it('should delete template', () => {
      // First add template to state
      service['_templates'].set([mockTemplate]);

      service.deleteTemplate('123').subscribe(() => {
        expect(service.templates()).toEqual([]);
      });

      const req = httpMock.expectOne('/api/workflow-templates/123');
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });

    it('should activate template', () => {
      const activatedTemplate = { ...mockTemplate, status: 'ACTIVE' as const };

      service.activateTemplate('123').subscribe(template => {
        expect(template.status).toBe('ACTIVE');
      });

      const req = httpMock.expectOne('/api/workflow-templates/123/activate');
      expect(req.request.method).toBe('POST');
      req.flush(activatedTemplate);
    });

    it('should clone template', () => {
      const clonedTemplate = { ...mockTemplate, id: '456', name: 'Template (Copy)' };

      service.cloneTemplate('123', 'Template (Copy)').subscribe(template => {
        expect(template.name).toBe('Template (Copy)');
      });

      const req = httpMock.expectOne('/api/workflow-templates/123/clone');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ name: 'Template (Copy)' });
      req.flush(clonedTemplate);
    });
  });

  describe('Task Operations', () => {
    beforeEach(() => {
      // Set current template
      service['_currentTemplate'].set({ ...mockTemplate, taskDefinitions: [] });
    });

    it('should add task', () => {
      const taskRequest = {
        name: 'Blood Test',
        taskType: 'MANUAL' as const,
        executionOrder: 1,
        isOptional: false,
        isMilestone: false
      };

      service.addTask('123', taskRequest).subscribe(task => {
        expect(task).toEqual(mockTask);
        expect(service.currentTemplate()?.taskDefinitions).toContain(mockTask);
      });

      const req = httpMock.expectOne('/api/workflow-templates/123/tasks');
      expect(req.request.method).toBe('POST');
      req.flush(mockTask);
    });

    it('should update task', () => {
      service['_currentTemplate'].set({ ...mockTemplate, taskDefinitions: [mockTask] });
      const updatedTask = { ...mockTask, name: 'Updated Blood Test' };

      service.updateTask('123', 'task-1', { name: 'Updated Blood Test' }).subscribe(task => {
        expect(task.name).toBe('Updated Blood Test');
      });

      const req = httpMock.expectOne('/api/workflow-templates/123/tasks/task-1');
      expect(req.request.method).toBe('PUT');
      req.flush(updatedTask);
    });

    it('should delete task', () => {
      service['_currentTemplate'].set({ ...mockTemplate, taskDefinitions: [mockTask] });

      service.deleteTask('123', 'task-1').subscribe(() => {
        expect(service.currentTemplate()?.taskDefinitions).toEqual([]);
      });

      const req = httpMock.expectOne('/api/workflow-templates/123/tasks/task-1');
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });

    it('should update task predecessors', () => {
      const updatedTask = { ...mockTask, predecessorTaskIds: ['task-2'] };

      service.updateTaskPredecessors('123', 'task-1', ['task-2']).subscribe(task => {
        expect(task.predecessorTaskIds).toContain('task-2');
      });

      const req = httpMock.expectOne('/api/workflow-templates/123/tasks/task-1/predecessors');
      expect(req.request.method).toBe('PUT');
      expect(req.request.body).toEqual(['task-2']);
      req.flush(updatedTask);
    });
  });

  describe('Notification Configuration', () => {
    beforeEach(() => {
      service['_currentTemplate'].set({ ...mockTemplate, taskDefinitions: [mockTask] });
    });

    it('should update task notification', () => {
      const config = {
        notificationType: 'KAFKA' as const,
        notificationKafkaTopic: 'lab-orders',
        notifyOnFailure: true,
        notifyOnSkip: false
      };

      const updatedTask = { ...mockTask, ...config };

      service.updateTaskNotification('123', 'task-1', config).subscribe(task => {
        expect(task.notificationType).toBe('KAFKA');
      });

      const req = httpMock.expectOne('/api/workflow-templates/123/tasks/task-1/notification');
      expect(req.request.method).toBe('PUT');
      req.flush(updatedTask);
    });

    it('should remove task notification', () => {
      service.removeTaskNotification('123', 'task-1').subscribe();

      const req = httpMock.expectOne('/api/workflow-templates/123/tasks/task-1/notification');
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });
  });

  describe('Computed Values', () => {
    it('should compute active templates', () => {
      const templates: WorkflowTemplate[] = [
        { ...mockTemplate, id: '1', status: 'ACTIVE' },
        { ...mockTemplate, id: '2', status: 'DRAFT' },
        { ...mockTemplate, id: '3', status: 'ACTIVE' }
      ];

      service['_templates'].set(templates);

      expect(service.activeTemplates()).toHaveLength(2);
      expect(service.activeTemplates().every(t => t.status === 'ACTIVE')).toBe(true);
    });

    it('should compute draft templates', () => {
      const templates: WorkflowTemplate[] = [
        { ...mockTemplate, id: '1', status: 'ACTIVE' },
        { ...mockTemplate, id: '2', status: 'DRAFT' },
        { ...mockTemplate, id: '3', status: 'DRAFT' }
      ];

      service['_templates'].set(templates);

      expect(service.draftTemplates()).toHaveLength(2);
      expect(service.draftTemplates().every(t => t.status === 'DRAFT')).toBe(true);
    });
  });

  describe('Error Handling', () => {
    it('should handle load templates error', () => {
      service.loadTemplates().subscribe(templates => {
        expect(templates).toEqual([]);
        expect(service.error()).toBe('Failed to load templates');
      });

      const req = httpMock.expectOne('/api/workflow-templates');
      req.error(new ErrorEvent('Network error'));
    });

    it('should clear error', () => {
      service['_error'].set('Some error');
      service.clearError();
      expect(service.error()).toBeNull();
    });
  });

  describe('State Management', () => {
    it('should set current template', () => {
      service.setCurrentTemplate(mockTemplate);
      expect(service.currentTemplate()).toEqual(mockTemplate);
    });

    it('should clear current template', () => {
      service.setCurrentTemplate(mockTemplate);
      service.setCurrentTemplate(null);
      expect(service.currentTemplate()).toBeNull();
    });
  });
});
