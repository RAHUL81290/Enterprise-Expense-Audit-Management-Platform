# API Examples

All requests go through the API Gateway at `http://localhost:8080` (or `docker compose`/`k8s` equivalent).

## Auth Service

### Register
```bash
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Rahul Kumar",
    "email": "rahul@example.com",
    "password": "secret123",
    "role": "USER"
  }'
```

### Register a manager (for approve/reject testing)
```bash
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Priya Singh",
    "email": "priya@example.com",
    "password": "secret123",
    "role": "MANAGER"
  }'
```

### Login
```bash
TOKEN=$(curl -s -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"rahul@example.com","password":"secret123"}' | jq -r '.token')

MANAGER_TOKEN=$(curl -s -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"priya@example.com","password":"secret123"}' | jq -r '.token')
```

## Expense Service

### Create an expense
```bash
curl -X POST http://localhost:8080/expenses \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"amount": 1500.00, "category": "TRAVEL", "description": "Flight to client site"}'
```

### Get my expenses (paginated)
```bash
curl "http://localhost:8080/expenses/my?page=0&size=5" \
  -H "Authorization: Bearer $TOKEN"
```

### Get all expenses (manager/admin only)
```bash
curl "http://localhost:8080/expenses?page=0&size=10" \
  -H "Authorization: Bearer $MANAGER_TOKEN"
```

### Get cached summary
```bash
curl http://localhost:8080/expenses/summary -H "Authorization: Bearer $TOKEN"
```

### Get expense enriched with employee details (Feign)
```bash
curl http://localhost:8080/expenses/1/with-user -H "Authorization: Bearer $TOKEN"
```

### Approve / Reject (manager only)
```bash
curl -X PUT http://localhost:8080/expenses/1/approve -H "Authorization: Bearer $MANAGER_TOKEN"
curl -X PUT http://localhost:8080/expenses/2/reject -H "Authorization: Bearer $MANAGER_TOKEN"
```

### Update / Delete
```bash
curl -X PUT http://localhost:8080/expenses/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"amount": 1600.00, "category": "TRAVEL", "description": "Flight to client site (updated)"}'

curl -X DELETE http://localhost:8080/expenses/1 -H "Authorization: Bearer $TOKEN"
```

## Audit Service

### Get one audit log
```bash
curl http://localhost:8080/audit-logs/1 -H "Authorization: Bearer $TOKEN"
```

### Filtered, paginated audit logs
```bash
curl "http://localhost:8080/audit-logs?event=EXPENSE_CREATED&page=0&size=10" \
  -H "Authorization: Bearer $TOKEN"

curl "http://localhost:8080/audit-logs?userId=1&event=EXPENSE_APPROVED&status=SUCCESS&page=0&size=10" \
  -H "Authorization: Bearer $TOKEN"

curl "http://localhost:8080/audit-logs?startDate=2026-09-01T00:00:00&endDate=2026-09-30T23:59:59&page=0&size=20&sort=timestamp,desc" \
  -H "Authorization: Bearer $TOKEN"
```

## Demonstrating resilience

### Kafka decoupling: stop audit-service, create an expense, restart audit-service
```bash
docker compose stop audit-service
curl -X POST http://localhost:8080/expenses \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
  -d '{"amount": 500.00, "category": "FOOD", "description": "Team lunch"}'
# Still 201 Created immediately.
docker compose start audit-service
# Within seconds, the audit log appears once the consumer catches up.
```

### Feign graceful degradation: stop auth-service, fetch enriched expense
```bash
docker compose stop auth-service
curl http://localhost:8080/expenses/1/with-user -H "Authorization: Bearer $TOKEN"
# Still 200 OK, with employeeName/employeeEmail = "Unknown"
docker compose start auth-service
```
