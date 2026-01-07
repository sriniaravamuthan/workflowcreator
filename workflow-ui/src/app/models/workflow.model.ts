// Workflow Template Models

export interface WorkflowTemplate {
  id: string;
  name: string;
  description?: string;
  version: string;
  status: TemplateStatus;
  category?: string;
  taskDefinitions: TaskDefinition[];
  createdAt: string;
  updatedAt: string;
  createdBy?: string;
  updatedBy?: string;
}

export interface TaskDefinition {
  id: string;
  name: string;
  description?: string;
  taskType: TaskType;
  executionOrder: number;
  isOptional: boolean;
  isMilestone: boolean;
  slaMinutes?: number;
  defaultAssigneeRole?: string;
  predecessorTaskIds: string[];
  nextTaskId?: string;

  // Visual positioning for canvas
  positionX?: number;
  positionY?: number;

  // Notification configuration
  notificationType?: NotificationType;
  notificationKafkaTopic?: string;
  notificationApiEndpoint?: string;
  notificationApiMethod?: string;
  notificationMessageTemplate?: string;
  notificationApiHeaders?: string;
  notifyOnFailure?: boolean;
  notifyOnSkip?: boolean;

  // Scheduling constraints
  schedulingConstraints?: string;
}

export interface TaskPosition {
  taskId: string;
  x: number;
  y: number;
}

export interface TaskConnection {
  fromTaskId: string;
  toTaskId: string;
}

export type TemplateStatus = 'DRAFT' | 'ACTIVE' | 'DEPRECATED' | 'ARCHIVED';

export type TaskType = 'MANUAL' | 'AUTOMATED' | 'APPROVAL' | 'NOTIFICATION' | 'INTEGRATION';

export type NotificationType = 'NONE' | 'KAFKA' | 'API' | 'BOTH';

// Form models
export interface CreateTemplateRequest {
  name: string;
  description?: string;
  category?: string;
}

export interface CreateTaskRequest {
  name: string;
  description?: string;
  taskType: TaskType;
  executionOrder: number;
  isOptional: boolean;
  isMilestone: boolean;
  slaMinutes?: number;
  defaultAssigneeRole?: string;
  predecessorTaskIds?: string[];
}

export interface UpdateTaskNotificationRequest {
  notificationType: NotificationType;
  notificationKafkaTopic?: string;
  notificationApiEndpoint?: string;
  notificationApiMethod?: string;
  notificationMessageTemplate?: string;
  notificationApiHeaders?: string;
  notifyOnFailure?: boolean;
  notifyOnSkip?: boolean;
}

// Canvas state
export interface CanvasState {
  zoom: number;
  panX: number;
  panY: number;
  selectedTaskId: string | null;
  isDragging: boolean;
}
