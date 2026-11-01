# demo

A small full-stack application for deleting files from a server-side upload directory, with a Spring Boot backend and a React + Vite frontend.

## Stack

Spring Boot 3.2 / Java 17 / React 18 / Vite 5

## Layout

```
.
├── backend/
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/example/demo/
│       │   ├── DemoApplication.java
│       │   ├── config/SecurityConfig.java
│       │   └── controller/ApiController.java
│       └── resources/application.properties
└── frontend/
    ├── index.html
    ├── package.json
    ├── vite.config.js
    └── src/
        ├── App.jsx
        ├── App.css
        └── main.jsx
```

## How to run

Backend:

```
cd backend
mvn spring-boot:run
```

The backend listens on `http://localhost:8080`. Set `APP_ADMIN_PASSWORD` to a strong value before starting (see Notes).

Frontend:

```
cd frontend
npm install
npm run dev
```

## API

All endpoints are under `/api`. Authentication is session-based (form login); CSRF protection is enabled via a `XSRF-TOKEN` cookie.

| Method | Path          | Auth required | Description                                              |
|--------|---------------|---------------|----------------------------------------------------------|
| POST   | `/api/login`  | No            | Form login (`username`, `password`). Returns 200 / 401.  |
| POST   | `/api/logout` | Yes           | Logs out the current session. Returns 200.               |
| POST   | `/api/delete` | `FILE_ADMIN`  | Deletes files. Body: `{ "paths": ["file1.txt", ...] }`.  |

`/api/delete` response:

```json
{
  "deleted": ["/var/app/uploads/file1.txt"],
  "failed": ["../etc/passwd (outside allowed directory)"]
}
```

## Notes

- Files are resolved relative to `/var/app/uploads` (hard-coded in `ApiController`). Paths that escape this directory, contain symlink components, or are not regular files are rejected.
- The admin user is in-memory and configured via `APP_ADMIN_USERNAME` (default `admin`) and `APP_ADMIN_PASSWORD`. The application refuses to start if `APP_ADMIN_PASSWORD` is unset, blank, or `change-me`.
- The frontend dev server is not configured with a proxy to the backend in this repository; requests to `/api/*` must be routed to `http://localhost:8080` (e.g. via a Vite proxy or reverse proxy) for the UI to work end-to-end.
- CSRF tokens must be sent with state-changing requests; the frontend is expected to read the `XSRF-TOKEN` cookie and echo it in the `X-XSRF-TOKEN` header.
- No persistence layer: users and file state are not stored in a database.
- Content Security Policy is strict (`default-src 'self'`); external scripts, styles, and fonts are not permitted.