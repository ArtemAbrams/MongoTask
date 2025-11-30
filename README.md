# Notes App - Spring Boot and MongoDB

A small REST API for storing notes with tags, pagination and per - note word statistics.

The project implements the requirements of the test assignment:

1) Spring Boot + MongoDB
2) Docker + Docker Compose
3) Test coverage: unit tests, MockMvc controller tests, integration tests with Testcontainers

---

## 1. How to run the application

### 1.1. Docker Compose

**Prerequisites:**

- Docker installed
- Docker Compose available

In the project root you should have:

- `docker-compose.yml`
- `Dockerfile`

Start everything with:

```bash
  docker compose build
  docker compose up
```

After startup:

- MongoDB runs in container `notes-mongo`
- The application runs in container `notes-app`
- API is available at: http://localhost:8080/api/notes

---

---

### 1.2. Run locally (without Docker Compose)

1. Start MongoDB locally:

   ```bash
   docker run -d --name notes-mongo -p 27017:27017 mongo:7
   ```

2. Configure the Mongo URI for Spring:
   The application uses:

   ```properties
   spring.data.mongodb.uri=${SPRING_DATA_MONGODB_URI:mongodb://localhost:27017/notesdb}
   ```

   That means, if `SPRING_DATA_MONGODB_URI` is set - it will be used, otherwise the app falls back to `mongodb://localhost:27017/notesdb`.

   **Linux / MacOS :**

   ```bash
   export SPRING_DATA_MONGODB_URI="mongodb://localhost:27017/notesdb"
   ```

   **Windows PowerShell:**

   ```powershell
   $env:SPRING_DATA_MONGODB_URI = "mongodb://localhost:27017/notesdb"
   ```

3. Run the Spring Boot application:

   ```bash
   mvn spring-boot:run
   ```

After that the API will be available at: http://localhost:8080/api/notes

---

## 2. How to run tests

Run all tests (unit, web, integration with Testcontainers):

```bash
  mvn test
```

Integration tests:

- start a MongoDB container via Testcontainers.
- override `spring.data.mongodb.uri` to point to this container.
- run end-to-end scenarios (create, read, list, delete, stats, error handling).

Tests **do not use** any production database - they run against an isolated MongoDB container.

---

## 3. REST API

Base URL:

```text
http://localhost:8080/api/notes
```

### 3.1. Create and update note

**POST** `/api/notes`

- `id` is missing -> create a new note
- `id` is present -> update an existing note

Example request:

```json
{
  "id": null,
  "title": "My first note",
  "text": "Today I wrote my first note",
  "tags": [
    "PERSONAL"
  ]
}
```

Validation:
- `title` - required
- `text` - required

Response (200 OK):

```json
{
  "id": "665f2e2fe4b0e12a12345678",
  "title": "My first note",
  "createdDate": "2025-02-27T00:00:00Z",
  "text": "Today I wrote my first note",
  "tags": [
    "PERSONAL"
  ]
}
```

---

### 3.2. Get note details

**GET** `/api/notes/{id}`

Example:

```text
GET /api/notes/665f2e2fe4b0e12a12345678
```

Response (200 OK):

```json
{
  "id": "665f2e2fe4b0e12a12345678",
  "title": "My first note",
  "createdDate": "2025-02-27T00:00:00Z",
  "text": "Today I wrote my first note",
  "tags": [
    "PERSONAL"
  ]
}
```

If the note is not found -> 404 (see Error handling).

---

### 3.3. List notes (summary + pagination + filter by tags)

**GET** `/api/notes`

Query parameters:

- `page` – page number (0 by default)
- `size` – page size (20 by default)
- `tags` – optional list of tags (`BUSINESS`, `PERSONAL`, `IMPORTANT`)

Examples:

```text
GET /api/notes?page=0&size=10
GET /api/notes?page=0&size=10&tags=BUSINESS&tags=IMPORTANT
```

Example response (200 OK, `Page<NoteSummaryResponse>`):

```json
{
  "content": [
    {
      "title": "My second note",
      "createdDate": "2025-02-28T00:00:00Z"
    },
    {
      "title": "My first note",
      "createdDate": "2025-02-27T00:00:00Z"
    }
  ],
  "totalElements": 2,
  "totalPages": 1,
  "size": 10,
  "number": 0
}
```

Details:

- only `title` and `createdDate` are return.
- results are always sorted by `createdDate` in descending order.
- if `tags` are not provided - all notes are return.
- if `tags` are provided - only notes with these tags are return.

---

### 3.4. Delete note

**DELETE** `/api/notes/{id}`

Example:

```text
DELETE /api/notes/665f2e2fe4b0e12a12345678
```

Response:

- 200 OK – note deleted.
- 404 Not Found – note does not exist.

---

### 3.5. Note text statistics

**GET** `/api/notes/{id}/stats`

Computes word statistics for the note text, counts occurrences of each unique token, ignoring case and punctuation, and returns the result sorted by frequency in
descending order. Numeric tokens are treated as words as well, since the requirements do not specify excluding them.

Example:

Text: `"note is just a note!"`

Response:

```json
{
  "wordStats": {
    "note": 2,
    "a": 1,
    "is": 1,
    "just": 1
  }
}
```

---

## 4. Error handling

A `GlobalExceptionHandler` maps errors to a unified `ApiError` response.

Examples:

### 4.1. 400 – Validation error

```json
{
  "occurredAt": "2025-11-30T10:15:30Z",
  "status": 400,
  "error": "Bad Request",
  "message": "title: title must not be blank; text: text must not be blank",
  "path": "/api/notes"
}
```

### 4.2. 404 – Not found

```json
{
  "occurredAt": "2025-11-30T10:15:30Z",
  "status": 404,
  "error": "Not Found",
  "message": "Note with id 123 not found",
  "path": "/api/notes/123"
}
```

### 4.3. 500 - Unexpected error

```json
{
  "occurredAt": "2025-11-30T10:15:30Z",
  "status": 500,
  "error": "Internal Server Error",
  "message": "Unexpected error occurred",
  "path": "/api/notes/123"
}
```
---

## 5. Swagger / API documentation

The project uses **springdoc-openapi** to expose OpenAPI documentation and Swagger UI.

Once the application is running:

- Swagger UI:

  ```text
  http://localhost:8080/swagger-ui/index.html
  ```

- OpenAPI JSON:

  ```text
  http://localhost:8080/v3/api-docs
  ```

Swagger UI lets you explore all endpoints, see request and response models and execute requests from the browser.

