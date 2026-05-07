# Environment Configuration Guide

## Overview
The UITMERCH backend uses environment variables for configuration management. This ensures sensitive data (API keys, database credentials) are never committed to version control.

## Setup Instructions

### 1. Copy the Template File
```bash
cd backend
cp .env.example .env
```

### 2. Fill in Environment Variables
Edit `.env` and provide actual values for your environment:

```bash
# Required Variables (no defaults)
APP_JWT_SECRET=your-generated-secret-key
SUPABASE_STORAGE_ENDPOINT=https://yourproject.supabase.co/storage/v1/s3
SUPABASE_PROJECT_REGION=ap-southeast-2
SUPABASE_STORAGE_S3_ACCESS_KEY_ID=your-key
SUPABASE_STORAGE_S3_SECRET_ACCESS_KEY=your-secret
APP_STORAGE_PUBLIC_URL=https://yourproject.supabase.co/storage/v1/object/public

# PostgreSQL Configuration
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/uitmerch
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres

# JWT Expiration (optional - uses defaults if not specified)
APP_JWT_EXPIRATION=86400000
APP_JWT_REFRESH_EXPIRATION=604800000
```

### 3. Generate JWT Secret (Development)
For a secure JWT secret, generate a random base64-encoded string:
```bash
openssl rand -base64 32
```
Copy the output into `APP_JWT_SECRET` in your `.env` file.

### 4. Load Environment Variables
The application will automatically load from `.env` via Spring Boot's `application.yaml` property resolution.

### 5. Verify Configuration
Run the application:
```bash
mvn spring-boot:run
```
If any required variables are missing, Spring Boot will fail at startup with a clear error message.

## Environment Variables Reference

### Server Configuration
- `SERVER_PORT` - Application port (default: 8080)

### JWT Configuration (NFR01)
- `APP_JWT_SECRET` - **REQUIRED** - JWT signing secret (≥256 bits)
- `APP_JWT_EXPIRATION` - Access token TTL in milliseconds (default: 86400000 = 24h)
- `APP_JWT_REFRESH_EXPIRATION` - Refresh token TTL in milliseconds (default: 604800000 = 7d)

### Database Configuration
- `SPRING_DATASOURCE_URL` - PostgreSQL JDBC URL (default: localhost:5432/uitmerch)
- `SPRING_DATASOURCE_USERNAME` - DB username (default: postgres)
- `SPRING_DATASOURCE_PASSWORD` - DB password (default: postgres)

### Supabase Storage Configuration (NFR03)
- `SUPABASE_STORAGE_ENDPOINT` - **REQUIRED** - S3-compatible endpoint
- `SUPABASE_PROJECT_REGION` - **REQUIRED** - AWS region (e.g., ap-southeast-2)
- `SUPABASE_STORAGE_S3_ACCESS_KEY_ID` - **REQUIRED** - S3 access key
- `SUPABASE_STORAGE_S3_SECRET_ACCESS_KEY` - **REQUIRED** - S3 secret key
- `APP_STORAGE_PUBLIC_URL` - **REQUIRED** - Public URL for uploaded files

### Logging Configuration (optional)
- `LOGGING_LEVEL_ROOT` - Root logging level (default: INFO)
- `LOGGING_LEVEL_COM_UITMERCH` - Application logging level (default: DEBUG)
- `LOGGING_LEVEL_ORG_HIBERNATE_SQL` - Hibernate SQL logging (default: DEBUG)
- `LOGGING_LEVEL_ORG_FLYWAYDB` - Flyway migration logging (default: DEBUG)

## Security Best Practices

### ✅ DO:
- Keep `.env` file locally only - never commit to Git
- Use strong, unique `APP_JWT_SECRET` in production
- Rotate `SUPABASE_STORAGE_S3_*` keys regularly
- Use different credentials for dev/staging/production
- Review `.env.example` before deploying to ensure all required vars are documented

### ❌ DON'T:
- Commit `.env` files to version control
- Share `.env` files via insecure channels
- Use development secrets in production
- Hardcode credentials in source code
- Use weak JWT secrets

## Docker/Container Deployment

When deploying with Docker, pass environment variables via:

```bash
docker run -e APP_JWT_SECRET="..." \
           -e SUPABASE_STORAGE_ENDPOINT="..." \
           -e SPRING_DATASOURCE_URL="..." \
           uitmerch-backend
```

Or use Docker Compose with environment file:
```yaml
services:
  backend:
    image: uitmerch-backend
    env_file:
      - .env
```

## Troubleshooting

### "Property not found" error
**Problem:** Spring Boot can't find a required environment variable.
**Solution:** Check that the variable is defined in `.env` and follows the correct name format.

### "Could not connect to database"
**Problem:** `SPRING_DATASOURCE_URL/USERNAME/PASSWORD` are incorrect.
**Solution:** Verify PostgreSQL is running and credentials match `psql` connection settings.

### "Access key invalid" for Supabase
**Problem:** S3 credentials were rejected.
**Solution:** Regenerate keys from Supabase dashboard and update `.env`.

## Reference

- [Spring Boot Configuration Properties](https://spring.io/projects/spring-boot)
- [Supabase Storage Documentation](https://supabase.com/docs/guides/storage)
- [PostgreSQL JDBC Driver](https://jdbc.postgresql.org/)
