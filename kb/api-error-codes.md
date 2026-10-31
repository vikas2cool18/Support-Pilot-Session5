# Acme Cloud API v2 – Error Codes

All API v2 errors return a JSON body with `code`, `message` and `requestId`. Quote the `requestId` when contacting support.

## Authentication errors

### ERR-4401 – Missing API token
The request has no `Authorization: Bearer <token>` header. Create a token in **Settings → API → Tokens**.

### ERR-4403 – Insufficient scope
The token is valid but lacks the scope needed for this endpoint, for example `invoices:write`. Create a new token with the required scopes.

### ERR-4471 – API token expired
The API token has passed its expiry date. Tokens expire after 90 days by default. Create a new token in **Settings → API → Tokens** and update your integration. Expired tokens cannot be renewed. Many integration failures that start overnight — such as sync jobs failing at a fixed time — are caused by ERR-4471.

### ERR-4472 – API token revoked
An admin revoked the token, or the user who created it was removed. Create a new token.

## Request errors

### ERR-4220 – Validation failed
One or more fields are invalid. The `details` array lists each field and the problem.

### ERR-4290 – Rate limit exceeded
Too many requests. Limits are 120 requests per minute on FREE, 600 on PRO and 3,000 on ENTERPRISE. Retry after the number of seconds in the `Retry-After` header.

### ERR-4130 – Payload too large
The request body exceeds 10 MB. Split large imports into smaller batches.

## Server errors

### ERR-5000 – Internal error
An unexpected error on our side. Retry with exponential back-off. If it persists, contact support with the `requestId`.

### ERR-5030 – Service unavailable
The service is temporarily unavailable, usually during an incident or maintenance. Check status.acmecloud.example.

## Deprecated

### ERR-4100 – API v1 deprecated
Returned by `/v1/...` endpoints with a warning header. API v1 will be switched off on 31 March 2027; migrate to `/v2/...`.
