# demo

A Spring Boot + React demo application for **safe file deletion**. The backend exposes an authenticated endpoint that deletes files within a configured base directory using `SecureDirectoryStream` to mitigate TOCTOU (time-of-check to time-of-use) attacks. The frontend is a minimal Vite + React UI.

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
│           │   ├── config/SecurityConfig.java
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

The backend requires the following environment variables:

- `ADMIN_PASSWORD` — **required**; the password for the in-memory `admin` user (BCrypt-hashed at startup).
- `DELETE_BASE_DIR` — optional; the directory under which deletions are permitted (defaults to `./data`).

Example:

```bash
ADMIN_PASSWORD=change-me DELETE_BASE_DIR=./data mvn spring-boot:run
```

The server listens on `http://localhost:8080`.

### Frontend

```bash
cd frontend
npm install
npm run dev
```

The Vite dev server runs on `http://localhost:5173` and proxies `/api` requests to `http://localhost:8080`.

## API

All endpoints require HTTP Basic authentication. Only the `admin` user (role `ADMIN`) is authorized; all other requests are denied.

| Method | Path          | Auth    | Description                                                       |
|--------|---------------|---------|-------------------------------------------------------------------|
| POST   | `/api/delete` | `ADMIN` | Delete one or more files relative to the configured base directory. |

### `POST /api/delete`

**Request body**

```json
{
  "paths": ["file1.txt", "subdir/file2.txt"]
}
```

**Response (200 OK)**

```json
{
  "deleted": ["file1.txt"],
  "failed": ["subdir/file2.txt"]
}
```

- `deleted` — paths that were successfully deleted.
- `failed` — paths that were rejected (invalid, outside the base directory, not a regular file, or deletion error).

**Error responses**

- `400 Bad Request` — `paths` is missing or null.
- `500 Internal Server Error` — the base directory is unavailable.

## Notes

- **Authentication**: Credentials are stored in memory and configured via the `ADMIN_PASSWORD` environment variable. The application will fail to start if it is not set.
- **Path safety**: Each requested path is resolved against the real (canonical) base directory and normalized. Paths that escape the base directory, resolve to the base itself, or are not regular files are rejected.
- **TOCTOU mitigation**: Deletion is performed via `SecureDirectoryStream` with `NOFOLLOW_LINKS` to avoid symlink traversal and race conditions.
- **Known limitation**: `SecureDirectoryStream` is not supported on all filesystems (notably some Windows configurations and certain network filesystems). When unavailable, deletions are refused rather than performed insecurely.
- **CSRF**: Disabled for the API; the endpoint is intended for programmatic/admin use behind HTTP Basic auth.
- **CORS**: Not configured on the backend; the Vite dev proxy is used to avoid cross-origin requests during development.