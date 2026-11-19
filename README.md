# PhishMeNot: Suspicious Link and Phishing Checker

IT342-G01 Systems Integration and Architecture 1 — individual project by Raphael Jude A. Alvarado.

PhishMeNot is a web-based phishing link checker. This repository currently contains
**Project Increment 1: User Accounts and Authentication**: account creation, sign-in/sign-out and
role-based dashboards for the two roles in the SRS (General User and Administrator).

| Layer    | Technology                                                                   |
|----------|------------------------------------------------------------------------------|
| Frontend | ReactJS 18 + Vite, React Router                                              |
| Backend  | Java 21, Spring Boot 3.4 (Web, Security, OAuth2 Client, Data JPA, Validation) |
| Database | MySQL 8                                                                      |
| Identity | Google OAuth 2.0 / OpenID Connect                                            |

## Increment 1 features

### 1. User account creation
PhishMeNot does not use passwords. The SRS requires Google sign-in only (FR-020, FR-021, FR-022,
NFR-005: *"the database shall contain no password fields"*), so registration happens automatically
on a user's **first Google sign-in**:

- **Required fields are validated** (`GoogleProfile`, Bean Validation): Google account id, a valid
  email address, a name, and a **Google-verified email**. If any of these fail, no account is
  created and the login page shows an error.
- **No duplicate accounts:** users are looked up by `google_id`; `google_id` and `email` are both
  unique keys in the `users` table, and an email already linked to a different Google account is
  rejected (`account_conflict`).
- **Stored in MySQL** in the `users` table (ERD, Figure 1): `user_id, google_id, email, name,
  profile_picture_url, role, created_at, last_login_at`.
- **Credentials:** Google checks the password, so PhishMeNot never receives or stores it. The only
  secrets are the OAuth client secret and DB password, and both are read from environment
  variables (NFR-007).
- **Role assignment:** new accounts get the `USER` role (General User). Administrators get `ADMIN`
  directly in the database (FR-046), or through the optional `ADMIN_EMAILS` setting (applied at
  sign-in, so it also promotes an account that already exists).

### 2. Login and logout
- The login page has a **Sign in with Google** button (`/oauth2/authorization/google`). Google's
  account chooser is always shown (`prompt=select_account`), so after signing out you can sign in
  with a different Google account.
- Spring Security runs the OAuth 2.0 authorization-code flow and checks the ID token.
- **Error messages** cover a cancelled sign-in, an unverified email, missing profile data, an email
  already linked to another account, and generic failures (`?error=<code>` on `/login`).
- **Sessions:** server-side HTTP session with an `HttpOnly`, `SameSite=Lax` cookie (`Secure` in
  production), a 30-minute idle timeout, and a new session id after login (session-fixation
  protection). State-changing requests need a CSRF token (`XSRF-TOKEN` cookie → `X-XSRF-TOKEN`
  header).
- **Logout:** `POST /api/auth/logout` invalidates the session and deletes the session cookie.
- **Protected pages and endpoints:** unauthenticated API calls return `401`. The React
  `ProtectedRoute` sends guests to `/login`.

### 3. Role-based dashboards
| Role           | Dashboard after login | Navigation                              | API access                       |
|----------------|-----------------------|-----------------------------------------|----------------------------------|
| General User   | `/dashboard`          | Dashboard                               | `/api/me`, `/api/user/**`        |
| Administrator  | `/admin`              | Admin Dashboard, Users, My Account      | everything above + `/api/admin/**` |

- **Administrator dashboard:** account statistics (total users, general users, administrators, new
  users in the last 7 days), the newest accounts, and a searchable, paginated list of all users.
- **Backend enforcement:** URL rules in `SecurityConfig` plus `@PreAuthorize("hasRole('ADMIN')")` on
  `AdminService`. A General User calling an admin endpoint gets **HTTP 403** (NFR-006).
- **Frontend enforcement:** `ProtectedRoute roles={[...]}` sends users without the right role to
  a 403 page.

## Increment 1 submission checklist

Status: **complete**. Git tag: `increment-1`. Every requirement below is implemented in this commit.

| Requirement | Where it is implemented |
|---|---|
| Account creation (Google sign-in), field validation | `backend/.../auth/UserAccountService.java`, `backend/.../auth/GoogleProfile.java` |
| No duplicate accounts | Unique keys in `backend/.../user/User.java` + checks in `UserAccountService.java` |
| Stored in MySQL | `backend/.../user/User.java`, `backend/.../user/UserRepository.java`, `application.properties` |
| No password storage (SRS FR-022, NFR-005) | Google verifies credentials; `users` table has no password column |
| Role assignment (USER / ADMIN) | `backend/.../user/Role.java`, `UserAccountService.java`, `ADMIN_EMAILS` |
| Login interface + error messages | `frontend/src/pages/LoginPage.jsx`, `backend/.../auth/LoginFailureHandler.java` |
| Secure sessions, CSRF, logout | `backend/.../config/SecurityConfig.java`, `frontend/src/api.js`, `frontend/src/auth/AuthContext.jsx` |
| Protected pages and endpoints | `SecurityConfig.java` (401/403), `frontend/src/components/ProtectedRoute.jsx` |
| Role-based redirect after login | `backend/.../auth/LoginSuccessHandler.java` |
| Role-based dashboards + navigation | `frontend/src/pages/UserDashboard.jsx`, `AdminDashboard.jsx`, `AdminUsers.jsx`, `frontend/src/components/Layout.jsx` |
| Backend RBAC | `SecurityConfig.java` URL rules + `@PreAuthorize` in `backend/.../admin/AdminService.java` |
| Tests | `backend/src/test/java/...` (17 tests: registration, duplicates, validation, 401/403, logout) |

`backend/...` = `backend/src/main/java/edu/cit/alvarado/phishmenot`.

## API

| Method | Path                               | Access        | Description                                  |
|--------|------------------------------------|---------------|----------------------------------------------|
| GET    | `/oauth2/authorization/google`     | Public        | Start Google sign-in                         |
| GET    | `/login/oauth2/code/google`        | Public        | OAuth callback (handled by Spring Security)  |
| POST   | `/api/auth/logout`                 | Signed in     | End the session (needs CSRF header) → 204    |
| GET    | `/api/me`                          | Signed in     | Current user's profile and role              |
| GET    | `/api/user/dashboard`              | USER, ADMIN   | General User dashboard data                  |
| GET    | `/api/admin/dashboard`             | ADMIN         | Account statistics + newest accounts         |
| GET    | `/api/admin/users?query=&page=&size=` | ADMIN      | Search / page through all users              |
| GET    | `/api/health`                      | Public        | Liveness check                               |

## Project structure

```
backend/                         Spring Boot REST API
  src/main/java/edu/cit/alvarado/phishmenot/
    auth/      Google sign-in: OIDC user service, account registration, login success/failure handlers, /api/me
    admin/     Administrator dashboard + user list (ADMIN only)
    user/      User entity, Role enum, repository, General User dashboard
    config/    SecurityConfig (OAuth2 login, sessions, CSRF, CORS, RBAC), app properties
    common/    Error handling, health endpoint
  src/test/    Integration tests (registration, duplicates, validation, 401/403, logout)
frontend/                        React + Vite single-page app
  src/auth/        AuthContext (session state, logout)
  src/components/  ProtectedRoute (role guard), Layout (role-based navigation), tables
  src/pages/       Login, User dashboard, Admin dashboard, Admin users, 403, 404
```

## Setup and execution

### Prerequisites
- JDK 21
- Node.js 20+ (LTS) and npm
- MySQL 8 running locally
- A Google Cloud project with an OAuth 2.0 Client ID

### 1. Create the Google OAuth client
1. Open Google Cloud Console → **APIs & Services → OAuth consent screen**, and configure it
   (External, add yourself as a test user).
2. Go to **Credentials → Create credentials → OAuth client ID → Web application**.
3. **Authorized JavaScript origins:** `http://localhost:5173`
4. **Authorized redirect URIs:** `http://localhost:8080/login/oauth2/code/google`
5. Copy the client ID and client secret.

### 2. Prepare MySQL
The database is created automatically (`createDatabaseIfNotExist=true`), and Hibernate creates the
`users` table on first start. You only need a MySQL user that is allowed to create it, for example:

```sql
CREATE DATABASE IF NOT EXISTS phishmenot;
CREATE USER 'phishmenot'@'localhost' IDENTIFIED BY 'change-me';
GRANT ALL PRIVILEGES ON phishmenot.* TO 'phishmenot'@'localhost';
```

### 3. Run the backend (port 8080)
Set the environment variables (see `backend/.env.example`), then start Spring Boot.

macOS / Linux:
```bash
cd backend
export GOOGLE_CLIENT_ID=xxxx.apps.googleusercontent.com
export GOOGLE_CLIENT_SECRET=xxxx
export DB_USERNAME=phishmenot
export DB_PASSWORD=change-me
export ADMIN_EMAILS=your.admin.email@gmail.com   # optional
./mvnw spring-boot:run
```

Windows (PowerShell):
```powershell
cd backend
$env:GOOGLE_CLIENT_ID="xxxx.apps.googleusercontent.com"
$env:GOOGLE_CLIENT_SECRET="xxxx"
$env:DB_USERNAME="phishmenot"
$env:DB_PASSWORD="change-me"
$env:ADMIN_EMAILS="your.admin.email@gmail.com"
.\mvnw.cmd spring-boot:run
```

In IntelliJ IDEA you can put the same variables in the run configuration's *Environment variables*.

| Variable               | Default                                            | Purpose                                    |
|------------------------|----------------------------------------------------|--------------------------------------------|
| `GOOGLE_CLIENT_ID`     | — (required)                                       | Google OAuth client id                     |
| `GOOGLE_CLIENT_SECRET` | — (required)                                       | Google OAuth client secret                 |
| `DB_URL`               | `jdbc:mysql://localhost:3306/phishmenot?createDatabaseIfNotExist=true&serverTimezone=UTC` | MySQL JDBC URL |
| `DB_USERNAME`          | `root`                                             | MySQL user                                 |
| `DB_PASSWORD`          | empty                                              | MySQL password                             |
| `FRONTEND_URL`         | `http://localhost:5173`                            | Where to redirect after login; CORS origin |
| `ADMIN_EMAILS`         | empty                                              | Comma-separated emails that get ADMIN at sign-in |
| `COOKIE_SECURE`        | `false`                                            | Set `true` when served over HTTPS          |
| `PORT`                 | `8080`                                             | Backend port                               |

### 4. Run the frontend (port 5173)
```bash
cd frontend
npm install
npm run dev
```
Open http://localhost:5173 and click **Sign in with Google**. In development, Vite proxies `/api`
to the backend (`VITE_BACKEND_URL`, default `http://localhost:8080`).

### 5. Make an existing account an Administrator
Either add the email to `ADMIN_EMAILS`, restart the backend, and sign in again, or update the role
in MySQL and sign in again:

```sql
UPDATE phishmenot.users SET role = 'ADMIN' WHERE email = 'your.admin.email@gmail.com';
```

### Run the tests
```bash
cd backend
./mvnw test
```
The tests use an in-memory H2 database in MySQL mode. They cover first-time registration,
returning users (no duplicates), duplicate-email rejection, required-field and verified-email
validation, admin role assignment, 401 for anonymous requests, 403 for General Users on admin
endpoints, CSRF-protected logout that ends the session, and role-based redirects after login.

## Demo walkthrough
1. Open http://localhost:5173 → you are sent to `/login`.
2. Sign in with a Google account → a `USER` account is created in MySQL → you land on `/dashboard`.
3. Try http://localhost:5173/admin → you are shown the **403** page. Calling
   `http://localhost:5173/api/admin/dashboard` directly returns HTTP 403 JSON.
4. Sign out → the session ends, and `/dashboard` sends you back to `/login`.
5. Promote the account (step 5 above), sign in again → you land on the **Administrator Dashboard**
   with the admin navigation menu.
