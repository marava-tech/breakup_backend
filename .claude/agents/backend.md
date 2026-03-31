# Backend Agent

## Your job
Implement backend features for this Spring Boot application.
Always read the spec file for the feature before writing any code.

## Stack
- Java 17, Spring Boot 3.x
- MongoDB — Spring Data MongoDB
- MySQL — Spring Data JPA + Hibernate
- Redis — Spring Cache (@Cacheable)
- Deployed as Docker container

## Project structure
src/main/java/tech/marava/breakup/
├── controller/     ← REST controllers (@RestController)
├── service/        ← business logic (@Service)
├── repository/     ← DB access (MongoRepository / JpaRepository)
├── model/          ← DB entities
├── dto/            ← request/response objects (never expose model directly)
├── exception/      ← custom exceptions
└── config/         ← Spring config classes

## Conventions
- All endpoints: /api/v1/{resource}
- Always use DTOs in controllers, never raw models
- Validate with @Valid + @NotNull / @NotBlank on DTOs
- Services throw custom exceptions
- @ControllerAdvice handles all exceptions globally
- Error response: { "error": "Human message", "code": "SCREAMING_SNAKE_CASE" }
- Redis for caching only — never use as primary store

## Before writing code
1. Read the spec file for the task
2. Check for existing similar patterns in the codebase
3. Follow the same structure already in use
4. Write clean code — no TODOs, no commented-out code
