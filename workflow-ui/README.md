# HMIS Workflow Builder UI

Angular 20 application for creating and managing workflow templates in the HMIS Workflow Engine.

## Features

- **Visual Workflow Canvas**: Drag-and-drop task nodes with connection lines
- **Template Management**: Create, edit, clone, and delete workflow templates
- **Task Configuration**: Configure task types, SLA, predecessors, and assignee roles
- **Notification Setup**: Configure Kafka and REST API notifications per task
- **Auto-Layout**: Automatic task positioning based on dependencies

## Prerequisites

- Node.js 20+
- npm 10+
- Angular CLI 20+

## Getting Started

### Install Dependencies

```bash
cd workflow-ui
npm install
```

### Development Server

```bash
npm start
# or
ng serve
```

Navigate to `http://localhost:4200/`. The application will automatically reload if you change any of the source files.

The development server proxies API requests to `http://localhost:8080` (Spring Boot backend).

### Build

```bash
npm run build
```

Build artifacts are stored in the `dist/workflow-ui` directory.

## Project Structure

```
workflow-ui/
├── src/
│   ├── app/
│   │   ├── components/
│   │   │   ├── workflow-list/       # Template list & create dialog
│   │   │   ├── workflow-canvas/     # Visual canvas & task creation
│   │   │   ├── task-node/           # Draggable task node component
│   │   │   ├── task-config-panel/   # Task configuration sidebar
│   │   │   └── notification-config/ # Notification settings
│   │   ├── pages/
│   │   │   ├── dashboard/           # Template list page
│   │   │   └── template-editor/     # Workflow editor page
│   │   ├── services/
│   │   │   ├── workflow.service.ts  # API integration
│   │   │   ├── canvas.service.ts    # Canvas state management
│   │   │   └── api.interceptor.ts   # HTTP interceptor
│   │   ├── models/
│   │   │   └── workflow.model.ts    # TypeScript interfaces
│   │   ├── app.component.ts         # Root component
│   │   └── app.routes.ts            # Route configuration
│   ├── environments/
│   ├── styles.scss                  # Global styles
│   └── index.html
├── angular.json
├── package.json
└── tsconfig.json
```

## API Integration

The UI connects to these backend endpoints:

### Templates
- `GET /api/workflow-templates` - List all templates
- `GET /api/workflow-templates/:id` - Get template details
- `POST /api/workflow-templates` - Create template
- `PUT /api/workflow-templates/:id` - Update template
- `DELETE /api/workflow-templates/:id` - Delete template
- `POST /api/workflow-templates/:id/activate` - Activate template
- `POST /api/workflow-templates/:id/clone` - Clone template

### Tasks
- `POST /api/workflow-templates/:id/tasks` - Add task
- `PUT /api/workflow-templates/:id/tasks/:taskId` - Update task
- `DELETE /api/workflow-templates/:id/tasks/:taskId` - Delete task
- `PUT /api/workflow-templates/:id/tasks/:taskId/predecessors` - Update predecessors
- `PUT /api/workflow-templates/:id/tasks/:taskId/notification` - Configure notifications

## Key Technologies

- **Angular 20** - Component framework with signals
- **Angular Material 20** - UI component library
- **RxJS** - Reactive state management
- **TypeScript 5.6** - Type safety
- **SCSS** - Styling

## Canvas Features

The workflow canvas supports:
- **Drag & Drop**: Move task nodes around the canvas
- **Zoom**: Mouse wheel to zoom in/out
- **Pan**: Click and drag on empty canvas to pan
- **Connections**: Drag from output handle to input handle to create dependencies
- **Auto-Layout**: Automatically arrange tasks based on predecessor relationships
- **Selection**: Click tasks to edit in the sidebar panel

## Configuration

### Proxy Configuration

Edit `proxy.conf.json` to change the backend API URL:

```json
{
  "/api": {
    "target": "http://localhost:8080",
    "secure": false
  }
}
```

### Environment Configuration

Edit `src/environments/environment.ts` for development settings.
