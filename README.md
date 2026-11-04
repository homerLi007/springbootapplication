# demo

A small full-stack application for safely deleting files and directories confined to a sandboxed root directory (`~/safe-delete-root`). The Spring Boot backend resolves each requested relative path against the sandbox, rejects path traversal attempts, and reports per-path results. The React frontend provides a simple textarea-based UI for submitting paths and viewing outcomes.

## Stack

Spring Boot 3.2 / Java 17 / React 18 / Vite 5

## Layout

```
.
├── backend/
│   ├── pom.xml
│   └── src/
│       └── main/
│           ├── java/com/example/demo/
│           │   ├── DemoApplication.java
│           │   └── controller/ApiController.java
│           └── resources/application.properties
└── frontend/
    ├── index.html
    ├── package.json
    ├── vite.config.js
    └── src/
        ├── main.jsx
        ├── App.jsx
        └── App.css
```

## How to run

Backend (listens on `http://localhost:8080`):

```bash
cd backend
mvn spring-boot:run
```

Frontend (dev server on `http://localhost:5173`, proxies `/api` to the backend):

```bash
cd frontend
npm install
npm run dev
```

## API

Base path: `/api`

### `POST /api/delete`

Deletes one or more paths, each resolved relative to `~/safe-delete-root`.

**Request body**

```json
{
  "paths": ["tmp/file.txt", "logs/old"]
}
```

**Response body**

```json
{
  "results": [
    { "path": "tmp/file.txt", "deleted": true, "error": null },
    { "path": "logs/old", "deleted": false, "error": "not found" }
  ]
}
```

**Behavior**

- Empty or blank paths return `deleted: false` with `"empty path"`.
- Paths that resolve outside `~/safe-delete-root` (e.g. `../etc`) are rejected with `"path escapes base directory"`.
- Missing paths return `"not found"`.
- Symbolic links are deleted as links (targets are not followed).
- Directories are deleted recursively.
- A malformed request (missing `paths`) returns `400 Bad Request`.

## Notes

- The sandbox root is fixed to `~/safe-delete-root` and is created lazily on first use; it is not configurable via properties.
- There is no authentication or authorization — do not expose this service on an untrusted network.
- Deletion is permanent; there is no trash/undo mechanism.
- Per-file errors during recursive directory deletion are silently ignored, so a directory may be reported as deleted even if some children could not be removed.
- CORS is not configured; the frontend relies on Vite's dev proxy. For production, serve the built frontend behind the same origin as the backend or add explicit CORS configuration.
- No persistence layer, tests beyond the default Spring Boot test starter, or containerization are included.