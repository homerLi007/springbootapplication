# demo

A small full-stack application for deleting files on the server by submitting a list of file paths from a React UI. The Spring Boot backend exposes a single REST endpoint that attempts to delete each provided path and reports which files were deleted and which failed.

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

### Backend

```bash
cd backend
mvn spring-boot:run
```

The API starts on `http://localhost:8080`.

### Frontend

```bash
cd frontend
npm install
npm run dev
```

The dev server starts on `http://localhost:5173` and proxies `/api` requests to the backend.

## API

Base path: `/api`

### `POST /api/delete`

Deletes the files at the given absolute paths.

**Request body**

```json
{
  "paths": ["/tmp/a.txt", "/tmp/b.txt"]
}
```

**Response `200 OK`**

```json
{
  "deleted": ["/tmp/a.txt"],
  "failed": ["/tmp/b.txt (not found)"]
}
```

**Response `400 Bad Request`** — returned when `paths` is missing:

```json
{ "error": "paths is required" }
```

## Notes

- **Destructive operation.** Files are permanently deleted with `Files.deleteIfExists`; there is no trash, undo, or confirmation step on the server side.
- **No authentication or authorization.** The endpoint accepts any path the backend process can access. Do not expose this service on an untrusted network.
- **No path allow-listing.** Paths are normalized to absolute form but not restricted to a sandbox directory, so any file writable by the backend user can be deleted.
- **Directories are not supported.** Only regular files are deleted; passing a directory path will result in a failure entry.
- **No persistence or logging.** Deletion results are returned to the caller but not stored.
- **CORS is not configured.** The frontend relies on the Vite dev proxy; a production deployment would need to serve the frontend from the same origin or add CORS configuration to the backend.