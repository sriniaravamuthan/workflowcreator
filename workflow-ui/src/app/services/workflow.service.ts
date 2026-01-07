import { Injectable, inject, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap, catchError, of } from 'rxjs';
import { environment } from '@env/environment';
import {
  WorkflowTemplate,
  TaskDefinition,
  CreateTemplateRequest,
  CreateTaskRequest,
  UpdateTaskNotificationRequest
} from '../models/workflow.model';

@Injectable({
  providedIn: 'root'
})
export class WorkflowService {
  private http = inject(HttpClient);
  private baseUrl = `${environment.apiUrl}/workflow-templates`;

  // Signals for reactive state management
  private _templates = signal<WorkflowTemplate[]>([]);
  private _currentTemplate = signal<WorkflowTemplate | null>(null);
  private _loading = signal(false);
  private _error = signal<string | null>(null);

  // Public readonly signals
  readonly templates = this._templates.asReadonly();
  readonly currentTemplate = this._currentTemplate.asReadonly();
  readonly loading = this._loading.asReadonly();
  readonly error = this._error.asReadonly();

  // Computed values
  readonly activeTemplates = computed(() =>
    this._templates().filter(t => t.status === 'ACTIVE')
  );

  readonly draftTemplates = computed(() =>
    this._templates().filter(t => t.status === 'DRAFT')
  );

  // ==================== Template Operations ====================

  loadTemplates(): Observable<WorkflowTemplate[]> {
    this._loading.set(true);
    this._error.set(null);

    return this.http.get<WorkflowTemplate[]>(this.baseUrl).pipe(
      tap(templates => {
        this._templates.set(templates);
        this._loading.set(false);
      }),
      catchError(err => {
        this._error.set('Failed to load templates');
        this._loading.set(false);
        console.error('Error loading templates:', err);
        return of([]);
      })
    );
  }

  getTemplate(id: string): Observable<WorkflowTemplate> {
    this._loading.set(true);

    return this.http.get<WorkflowTemplate>(`${this.baseUrl}/${id}`).pipe(
      tap(template => {
        this._currentTemplate.set(template);
        this._loading.set(false);
      }),
      catchError(err => {
        this._error.set('Failed to load template');
        this._loading.set(false);
        throw err;
      })
    );
  }

  createTemplate(request: CreateTemplateRequest): Observable<WorkflowTemplate> {
    return this.http.post<WorkflowTemplate>(this.baseUrl, request).pipe(
      tap(template => {
        this._templates.update(templates => [...templates, template]);
        this._currentTemplate.set(template);
      })
    );
  }

  updateTemplate(id: string, updates: Partial<WorkflowTemplate>): Observable<WorkflowTemplate> {
    return this.http.put<WorkflowTemplate>(`${this.baseUrl}/${id}`, updates).pipe(
      tap(template => {
        this._templates.update(templates =>
          templates.map(t => t.id === id ? template : t)
        );
        this._currentTemplate.set(template);
      })
    );
  }

  deleteTemplate(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`).pipe(
      tap(() => {
        this._templates.update(templates =>
          templates.filter(t => t.id !== id)
        );
        if (this._currentTemplate()?.id === id) {
          this._currentTemplate.set(null);
        }
      })
    );
  }

  activateTemplate(id: string): Observable<WorkflowTemplate> {
    return this.http.post<WorkflowTemplate>(`${this.baseUrl}/${id}/activate`, {}).pipe(
      tap(template => {
        this._templates.update(templates =>
          templates.map(t => t.id === id ? template : t)
        );
        this._currentTemplate.set(template);
      })
    );
  }

  cloneTemplate(id: string, newName: string): Observable<WorkflowTemplate> {
    return this.http.post<WorkflowTemplate>(`${this.baseUrl}/${id}/clone`, { name: newName }).pipe(
      tap(template => {
        this._templates.update(templates => [...templates, template]);
      })
    );
  }

  // ==================== Task Operations ====================

  addTask(templateId: string, task: CreateTaskRequest): Observable<TaskDefinition> {
    return this.http.post<TaskDefinition>(`${this.baseUrl}/${templateId}/tasks`, task).pipe(
      tap(newTask => {
        this._currentTemplate.update(template => {
          if (!template) return null;
          return {
            ...template,
            taskDefinitions: [...template.taskDefinitions, newTask]
          };
        });
      })
    );
  }

  updateTask(templateId: string, taskId: string, updates: Partial<TaskDefinition>): Observable<TaskDefinition> {
    return this.http.put<TaskDefinition>(`${this.baseUrl}/${templateId}/tasks/${taskId}`, updates).pipe(
      tap(updatedTask => {
        this._currentTemplate.update(template => {
          if (!template) return null;
          return {
            ...template,
            taskDefinitions: template.taskDefinitions.map(t =>
              t.id === taskId ? updatedTask : t
            )
          };
        });
      })
    );
  }

  deleteTask(templateId: string, taskId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${templateId}/tasks/${taskId}`).pipe(
      tap(() => {
        this._currentTemplate.update(template => {
          if (!template) return null;
          return {
            ...template,
            taskDefinitions: template.taskDefinitions.filter(t => t.id !== taskId)
          };
        });
      })
    );
  }

  updateTaskPredecessors(templateId: string, taskId: string, predecessorIds: string[]): Observable<TaskDefinition> {
    return this.http.put<TaskDefinition>(
      `${this.baseUrl}/${templateId}/tasks/${taskId}/predecessors`,
      predecessorIds
    ).pipe(
      tap(updatedTask => {
        this._currentTemplate.update(template => {
          if (!template) return null;
          return {
            ...template,
            taskDefinitions: template.taskDefinitions.map(t =>
              t.id === taskId ? updatedTask : t
            )
          };
        });
      })
    );
  }

  // ==================== Notification Configuration ====================

  updateTaskNotification(
    templateId: string,
    taskId: string,
    config: UpdateTaskNotificationRequest
  ): Observable<TaskDefinition> {
    return this.http.put<TaskDefinition>(
      `${this.baseUrl}/${templateId}/tasks/${taskId}/notification`,
      config
    ).pipe(
      tap(updatedTask => {
        this._currentTemplate.update(template => {
          if (!template) return null;
          return {
            ...template,
            taskDefinitions: template.taskDefinitions.map(t =>
              t.id === taskId ? updatedTask : t
            )
          };
        });
      })
    );
  }

  removeTaskNotification(templateId: string, taskId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${templateId}/tasks/${taskId}/notification`);
  }

  // ==================== Utility Methods ====================

  setCurrentTemplate(template: WorkflowTemplate | null): void {
    this._currentTemplate.set(template);
  }

  clearError(): void {
    this._error.set(null);
  }
}
