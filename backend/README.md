# Backend

Spring Boot REST backend for reserve events management.

## Features
- Reserves and events management
- Event geographic validation inside reserve boundaries
- JWT authentication for Admin/Inspector users
- Role-based authorization
- Admin-to-reserve access restrictions
- Event lifecycle logging (`event_logs`)
- Flyway migrations with initial seed data

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
