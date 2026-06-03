# WhatsApp Engine

Backend-only WhatsApp SaaS engine built with Spring Boot 3, Java 17, PostgreSQL, Flyway, JWT, Spring Security, and Meta WhatsApp Cloud API.

## Production Features

- Modular monolith package layout
- Tenant-scoped organizations, users, contacts, messages, campaigns, and webhooks
- JWT authentication with BCrypt password hashing
- Role-based access control
- Versioned REST API under `/api/v1`
- Structured console logs with `correlationId`
- `X-Correlation-ID` request/response support
- Request rate limiting
- Persistent audit logs
- Flyway database migrations
- PostgreSQL configuration through environment variables
- OpenAPI/Swagger UI
- Docker and Docker Compose support

## Requirements

- JDK 17
- Docker, optional
- PostgreSQL 14+

## Environment

Create a local `.env` from the template:

```powershell
Copy-Item .env.example .env
```

Update secrets before production:

- `DATABASE_URL`
- `DATABASE_USERNAME`
- `DATABASE_PASSWORD`
- `JWT_SECRET`
- `META_WHATSAPP_WEBHOOK_VERIFY_TOKEN`
- `CORS_ALLOWED_ORIGINS`

## Run Locally

```powershell
.\mvnw.cmd spring-boot:run
```

Default base URL:

```text
http://localhost:8080/api/v1
```

Swagger:

```text
http://localhost:8080/api/v1/swagger-ui.html
```

## Run With Docker Compose

```powershell
Copy-Item .env.example .env
docker compose up --build
```

## Core Endpoints

- `POST /auth/register`
- `POST /auth/login`
- `POST /contacts`
- `GET /contacts`
- `POST /contacts/upload`
- `POST /messages/text`
- `POST /messages/template`
- `GET /webhooks/meta`
- `POST /webhooks/meta`
- `POST /campaigns`
- `GET /campaigns`
- `GET /campaigns/{id}`
- `POST /campaigns/{id}/launch`
- `GET /reports/summary`
- `GET /reports/messages`
- `GET /reports/messages/{id}`
- `GET /reports/status-breakdown`
- `GET /reports/daily-trend`
- `GET /reports/export/csv`
- `GET /reports/export/excel`

All paths are relative to `/api/v1`.

## Message Analytics

Report endpoints support these optional filters:

```text
dateFrom=2026-06-01T00:00:00Z
dateTo=2026-06-30T23:59:59Z
templateName=order_update
campaignId=<uuid>
contactId=<uuid>
status=DELIVERED
```

Dashboard summary response:

```json
{
  "totalSent": 12500,
  "totalDelivered": 11800,
  "totalRead": 9400,
  "totalFailed": 700,
  "deliveryRate": 94.4,
  "readRate": 79.66,
  "failureRate": 5.6,
  "messagesSentToday": 320,
  "messagesDeliveredToday": 301,
  "messagesReadToday": 250
}
```

Meta status webhook callbacks are matched by `statuses[].id` against the stored WhatsApp message ID. Duplicate callbacks for the same status and timestamp are ignored.

Sample Meta webhook status payload:

```json
{
  "object": "whatsapp_business_account",
  "entry": [
    {
      "changes": [
        {
          "field": "messages",
          "value": {
            "metadata": {
              "phone_number_id": "123456789"
            },
            "statuses": [
              {
                "id": "wamid.HBgM...",
                "status": "delivered",
                "timestamp": "1717248000"
              }
            ]
          }
        }
      ]
    }
  ]
}
```

## Correlation ID

Clients may send:

```text
X-Correlation-ID: your-request-id
```

If omitted, the API generates one and includes it in the response.

## Pagination

List endpoints use Spring pagination:

```text
?page=0&size=20&sort=createdAt,desc
```

The maximum page size is controlled by `MAX_PAGE_SIZE`.

## CSV Contacts Upload

Upload `multipart/form-data` with field name `file`.

Required headers:

```csv
name,phoneNumber
```

Optional headers:

```csv
email,notes
```

## Security Notes

- Keep `.env` out of source control.
- Use a strong `JWT_SECRET` of at least 256 bits.
- Restrict `CORS_ALLOWED_ORIGINS` in production.
- Terminate TLS at the load balancer or ingress.
- Store tenant WhatsApp access tokens securely for production deployments.
