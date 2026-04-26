# Backend

Spring Boot REST backend for reserve events management.

## Features
- Reserves and events management
- Event geographic validation inside reserve boundaries
- JWT authentication for admin and manager users
- Role-based authorization
- Manager-to-reserve access restrictions
- Event lifecycle logging (`event_logs`)
- Flyway migrations with initial seed data

## Related Docs

- [Backend Block Diagram](../docs/backend-block-diagram.md)
- [Database Block Diagram](../docs/database-block-diagram.md)
- [System Architecture Planning Document](../docs/system-architecture-planning.md)

## Run
```bash
docker compose up -d
./mvnw spring-boot:run
```

## Auth
`POST /api/auth/login`

```json
{
  "email": "admin@reserve.local",
  "password": "ChangeMe123!"
}
```
