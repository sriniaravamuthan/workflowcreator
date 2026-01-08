import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of, throwError } from 'rxjs';

import { DashboardComponent } from './dashboard.component';
import { WorkflowService } from '../../services/workflow.service';
import { WorkflowTemplate } from '../../models/workflow.model';

describe('DashboardComponent', () => {
  let component: DashboardComponent;
  let fixture: ComponentFixture<DashboardComponent>;
  let workflowService: jasmine.SpyObj<WorkflowService>;
  let dialog: jasmine.SpyObj<MatDialog>;
  let snackBar: jasmine.SpyObj<MatSnackBar>;

  const mockTemplates: WorkflowTemplate[] = [
    {
      id: '1',
      name: 'Template 1',
      description: 'First template',
      version: '1.0',
      status: 'ACTIVE',
      category: 'ADMISSION',
      taskDefinitions: [],
      createdAt: '2024-01-01T00:00:00',
      updatedAt: '2024-01-01T00:00:00'
    },
    {
      id: '2',
      name: 'Template 2',
      description: 'Second template',
      version: '1.0',
      status: 'DRAFT',
      category: 'SURGICAL',
      taskDefinitions: [],
      createdAt: '2024-01-02T00:00:00',
      updatedAt: '2024-01-02T00:00:00'
    }
  ];

  beforeEach(async () => {
    const workflowServiceSpy = jasmine.createSpyObj('WorkflowService', [
      'loadTemplates',
      'createTemplate',
      'deleteTemplate',
      'activateTemplate',
      'cloneTemplate',
      'templates',
      'loading',
      'error'
    ]);

    // Set up signal-like behavior
    workflowServiceSpy.templates.and.returnValue(mockTemplates);
    workflowServiceSpy.loading.and.returnValue(false);
    workflowServiceSpy.error.and.returnValue(null);
    workflowServiceSpy.loadTemplates.and.returnValue(of(mockTemplates));

    const dialogSpy = jasmine.createSpyObj('MatDialog', ['open']);
    const snackBarSpy = jasmine.createSpyObj('MatSnackBar', ['open']);

    await TestBed.configureTestingModule({
      imports: [
        DashboardComponent,
        NoopAnimationsModule
      ],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: WorkflowService, useValue: workflowServiceSpy },
        { provide: MatDialog, useValue: dialogSpy },
        { provide: MatSnackBar, useValue: snackBarSpy }
      ]
    }).compileComponents();

    workflowService = TestBed.inject(WorkflowService) as jasmine.SpyObj<WorkflowService>;
    dialog = TestBed.inject(MatDialog) as jasmine.SpyObj<MatDialog>;
    snackBar = TestBed.inject(MatSnackBar) as jasmine.SpyObj<MatSnackBar>;

    fixture = TestBed.createComponent(DashboardComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load templates on init', () => {
    fixture.detectChanges();
    expect(workflowService.loadTemplates).toHaveBeenCalled();
  });

  describe('openCreateDialog', () => {
    it('should open create template dialog', () => {
      const dialogRefSpy = jasmine.createSpyObj('MatDialogRef', ['afterClosed']);
      dialogRefSpy.afterClosed.and.returnValue(of(null));
      dialog.open.and.returnValue(dialogRefSpy);

      component.openCreateDialog();

      expect(dialog.open).toHaveBeenCalled();
    });

    it('should create template when dialog returns result', () => {
      const newTemplate = { name: 'New Template', description: 'Desc' };
      const createdTemplate = { ...mockTemplates[0], ...newTemplate };

      const dialogRefSpy = jasmine.createSpyObj('MatDialogRef', ['afterClosed']);
      dialogRefSpy.afterClosed.and.returnValue(of(newTemplate));
      dialog.open.and.returnValue(dialogRefSpy);
      workflowService.createTemplate.and.returnValue(of(createdTemplate));

      component.openCreateDialog();

      expect(workflowService.createTemplate).toHaveBeenCalledWith(newTemplate);
      expect(snackBar.open).toHaveBeenCalledWith('Template created successfully', 'Close', { duration: 3000 });
    });

    it('should show error when create fails', () => {
      const newTemplate = { name: 'New Template' };

      const dialogRefSpy = jasmine.createSpyObj('MatDialogRef', ['afterClosed']);
      dialogRefSpy.afterClosed.and.returnValue(of(newTemplate));
      dialog.open.and.returnValue(dialogRefSpy);
      workflowService.createTemplate.and.returnValue(throwError(() => new Error('Failed')));

      component.openCreateDialog();

      expect(snackBar.open).toHaveBeenCalledWith('Failed to create template', 'Close', { duration: 3000 });
    });
  });

  describe('cloneTemplate', () => {
    it('should clone template', () => {
      const template = mockTemplates[0];
      const clonedTemplate = { ...template, id: '3', name: 'Template 1 (Copy)' };
      workflowService.cloneTemplate.and.returnValue(of(clonedTemplate));

      component.cloneTemplate(template);

      expect(workflowService.cloneTemplate).toHaveBeenCalledWith('1', 'Template 1 (Copy)');
      expect(snackBar.open).toHaveBeenCalledWith('Template cloned successfully', 'Close', { duration: 3000 });
    });

    it('should show error when clone fails', () => {
      const template = mockTemplates[0];
      workflowService.cloneTemplate.and.returnValue(throwError(() => new Error('Failed')));

      component.cloneTemplate(template);

      expect(snackBar.open).toHaveBeenCalledWith('Failed to clone template', 'Close', { duration: 3000 });
    });
  });

  describe('activateTemplate', () => {
    it('should activate template', () => {
      const template = mockTemplates[1]; // DRAFT template
      const activatedTemplate = { ...template, status: 'ACTIVE' as const };
      workflowService.activateTemplate.and.returnValue(of(activatedTemplate));

      component.activateTemplate(template);

      expect(workflowService.activateTemplate).toHaveBeenCalledWith('2');
      expect(snackBar.open).toHaveBeenCalledWith('Template activated', 'Close', { duration: 3000 });
    });

    it('should show error when activation fails', () => {
      const template = mockTemplates[1];
      workflowService.activateTemplate.and.returnValue(throwError(() => new Error('Failed')));

      component.activateTemplate(template);

      expect(snackBar.open).toHaveBeenCalledWith('Failed to activate template', 'Close', { duration: 3000 });
    });
  });

  describe('deleteTemplate', () => {
    it('should delete template after confirmation', () => {
      spyOn(window, 'confirm').and.returnValue(true);
      const template = mockTemplates[0];
      workflowService.deleteTemplate.and.returnValue(of(void 0));

      component.deleteTemplate(template);

      expect(workflowService.deleteTemplate).toHaveBeenCalledWith('1');
      expect(snackBar.open).toHaveBeenCalledWith('Template deleted', 'Close', { duration: 3000 });
    });

    it('should not delete template if not confirmed', () => {
      spyOn(window, 'confirm').and.returnValue(false);
      const template = mockTemplates[0];

      component.deleteTemplate(template);

      expect(workflowService.deleteTemplate).not.toHaveBeenCalled();
    });

    it('should show error when delete fails', () => {
      spyOn(window, 'confirm').and.returnValue(true);
      const template = mockTemplates[0];
      workflowService.deleteTemplate.and.returnValue(throwError(() => new Error('Failed')));

      component.deleteTemplate(template);

      expect(snackBar.open).toHaveBeenCalledWith('Failed to delete template', 'Close', { duration: 3000 });
    });
  });
});
