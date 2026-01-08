import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TaskNodeComponent } from './task-node.component';
import { TaskDefinition } from '../../models/workflow.model';

describe('TaskNodeComponent', () => {
  let component: TaskNodeComponent;
  let fixture: ComponentFixture<TaskNodeComponent>;

  const mockTask: TaskDefinition = {
    id: 'task-1',
    name: 'Blood Test',
    description: 'Take blood sample',
    taskType: 'MANUAL',
    executionOrder: 1,
    isOptional: false,
    isMilestone: false,
    slaMinutes: 60,
    predecessorTaskIds: ['task-0']
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TaskNodeComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(TaskNodeComponent);
    component = fixture.componentInstance;
    component.task = mockTask;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('getTaskIcon', () => {
    it('should return person icon for MANUAL task', () => {
      component.task = { ...mockTask, taskType: 'MANUAL' };
      expect(component.getTaskIcon()).toBe('person');
    });

    it('should return smart_toy icon for AUTOMATED task', () => {
      component.task = { ...mockTask, taskType: 'AUTOMATED' };
      expect(component.getTaskIcon()).toBe('smart_toy');
    });

    it('should return thumb_up icon for APPROVAL task', () => {
      component.task = { ...mockTask, taskType: 'APPROVAL' };
      expect(component.getTaskIcon()).toBe('thumb_up');
    });

    it('should return notifications icon for NOTIFICATION task', () => {
      component.task = { ...mockTask, taskType: 'NOTIFICATION' };
      expect(component.getTaskIcon()).toBe('notifications');
    });

    it('should return sync_alt icon for INTEGRATION task', () => {
      component.task = { ...mockTask, taskType: 'INTEGRATION' };
      expect(component.getTaskIcon()).toBe('sync_alt');
    });
  });

  describe('formatSLA', () => {
    it('should format minutes less than 60', () => {
      expect(component.formatSLA(30)).toBe('30m');
      expect(component.formatSLA(45)).toBe('45m');
    });

    it('should format exactly 60 minutes as 1h', () => {
      expect(component.formatSLA(60)).toBe('1h');
    });

    it('should format hours with remaining minutes', () => {
      expect(component.formatSLA(90)).toBe('1h 30m');
      expect(component.formatSLA(150)).toBe('2h 30m');
    });

    it('should format full hours without minutes', () => {
      expect(component.formatSLA(120)).toBe('2h');
      expect(component.formatSLA(180)).toBe('3h');
    });
  });

  describe('Events', () => {
    it('should emit dragStart on mousedown', () => {
      spyOn(component.dragStart, 'emit');

      const event = new MouseEvent('mousedown', {
        clientX: 100,
        clientY: 200
      });

      component.onMouseDown(event);

      expect(component.dragStart.emit).toHaveBeenCalledWith({ x: 100, y: 200 });
    });

    it('should emit connectStart on connection handle mousedown', () => {
      spyOn(component.connectStart, 'emit');

      const event = new MouseEvent('mousedown');
      component.onConnectHandleDown(event);

      expect(component.connectStart.emit).toHaveBeenCalled();
    });

    it('should emit connectEnd on connection handle mouseup', () => {
      spyOn(component.connectEnd, 'emit');

      component.onConnectHandleUp();

      expect(component.connectEnd.emit).toHaveBeenCalled();
    });

    it('should emit dragEnd on mouseup after dragging', () => {
      spyOn(component.dragEnd, 'emit');

      // Start drag
      const startEvent = new MouseEvent('mousedown', { clientX: 100, clientY: 200 });
      component.onMouseDown(startEvent);

      // End drag
      component.onMouseUp();

      expect(component.dragEnd.emit).toHaveBeenCalled();
    });

    it('should not emit dragEnd if not dragging', () => {
      spyOn(component.dragEnd, 'emit');

      component.onMouseUp();

      expect(component.dragEnd.emit).not.toHaveBeenCalled();
    });
  });

  describe('Rendering', () => {
    it('should display task name', () => {
      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.querySelector('.task-title')?.textContent).toContain('Blood Test');
    });

    it('should show SLA badge when slaMinutes is set', () => {
      component.task = { ...mockTask, slaMinutes: 60 };
      fixture.detectChanges();

      const compiled = fixture.nativeElement as HTMLElement;
      const slaBadge = compiled.querySelector('.task-badge');
      expect(slaBadge?.textContent).toContain('1h');
    });

    it('should show optional badge for optional tasks', () => {
      component.task = { ...mockTask, isOptional: true };
      fixture.detectChanges();

      const compiled = fixture.nativeElement as HTMLElement;
      const badges = compiled.querySelectorAll('.task-badge');
      const hasOptionalBadge = Array.from(badges).some(b => b.textContent?.includes('Optional'));
      expect(hasOptionalBadge).toBe(true);
    });

    it('should show milestone badge for milestone tasks', () => {
      component.task = { ...mockTask, isMilestone: true };
      fixture.detectChanges();

      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.querySelector('.task-node')?.classList.contains('milestone')).toBe(true);
    });

    it('should show predecessor count when predecessors exist', () => {
      component.task = { ...mockTask, predecessorTaskIds: ['task-1', 'task-2'] };
      fixture.detectChanges();

      const compiled = fixture.nativeElement as HTMLElement;
      const count = compiled.querySelector('.predecessor-count');
      expect(count?.textContent).toContain('2');
    });

    it('should apply selected class when isSelected is true', () => {
      component.isSelected = true;
      fixture.detectChanges();

      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.querySelector('.task-node')?.classList.contains('selected')).toBe(true);
    });

    it('should apply optional class for optional tasks', () => {
      component.task = { ...mockTask, isOptional: true };
      fixture.detectChanges();

      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.querySelector('.task-node')?.classList.contains('optional')).toBe(true);
    });
  });
});
