---
description: "Use when working on backend Spring Boot code, REST APIs, Flyway migrations, PostgreSQL/Supabase persistence, validation, error handling, or feature-based Java modules."
name: "Backend Coding Agent"
applyTo: "backend/src/{main,test}/**/*.{java,yaml}"
---
# Backend Coding Agent Instructions

- Target Spring Boot 3, Java 21, PostgreSQL on Supabase, Flyway migrations, and REST APIs.
- Keep the codebase feature-based and modular. Each module should group controller, service, repository, entity, and dto packages together.
- Keep controllers thin. They should only handle request and response concerns.
- Put business logic, validation, and repository coordination in services.
- Keep repositories limited to JPA data access.
- Use request/response DTOs and never expose entities directly from APIs.
- Map entities with JPA annotations and align them with database tables.
- Use naming that follows XxxController, XxxService, XxxRepository, XxxEntity, XxxRequest, and XxxResponse.
- Use Flyway for schema changes. Never modify old migration files; add new migrations instead.
- Use UUID primary keys.
- Keep APIs under the /api base path and follow RESTful conventions with proper HTTP status codes.
- Use @Valid and Jakarta Validation annotations for input validation.
- Use a global exception handler. Do not throw raw exceptions from the API layer.
- Plan for JWT-based authentication, but do not introduce it unless the task asks for security work.
- Do not mix layers, put logic in controllers, or return entities directly.
