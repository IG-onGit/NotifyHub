# NotifyHub - Backend setup instructions for integrating NotifyHub as mobile notification receiver.

NotifyHub is an Android app that **polls your server** and shows what it finds as system notifications. It never pushes to your server and talks to no third party. You need to set up:

| # | Endpoint | Called by | Required? |
|---|----------|-----------|-----------|
| 1 | `GET /api/notifications?since=<epochMillis>` | The Android app (every poll) | **Yes** |
| 2 | `POST /api/notifications` | Your own services/scripts | Recommended (how data gets in) |

Use **HTTPS** in production. The bearer token is sent on every request.

---

## 1. `GET /api/notifications` (required - the app calls this)

Returns every notification strictly newer than `since`.

### Request

| Part | Value |
|------|-------|
| Query `since` | Epoch **milliseconds**. Return only notifications with `timestamp > since`. `0` on first run / after a clear. |
| Header `Authorization` | `Bearer <token>` - sent only if the user set a token in the app. Validate it and return `401` if wrong. |

```bash
# First run (fetch everything)
curl -X GET "https://your-server.example.com/api/notifications?since=0" \
  -H "Authorization: Bearer YOUR_TOKEN"

# Subsequent poll (only newer than a known timestamp)
curl -X GET "https://your-server.example.com/api/notifications?since=1739203200000" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### Response - `200 OK`, `Content-Type: application/json`

```json
{
  "notifications": [
    {
      "id": "a-unique-stable-id",
      "projectKey": "my-web-app",
      "title": "Deploy finished",
      "message": "v2.4.1 deployed to production in 38s",
      "timestamp": 1739203200000
    }
  ]
}
```

Nothing new → return `{"notifications": []}` with `200`. This is the normal case on most polls; do **not** return an error or `204`.

### Field rules

| Field | Type | Rule |
|-------|------|------|
| `id` | string | Unique and **stable**. The app de-duplicates on it. Never reuse an id, never change it on retry. |
| `projectKey` | string | Must exactly match the key registered in the app's Projects screen. Unmatched keys still show, with a default bell icon/colour. |
| `title` | string | Notification title. |
| `message` | string | Notification body. |
| `timestamp` | number | Epoch **milliseconds**. Newer notifications must have a strictly larger value (see note below). |

All five fields are required in every item.

---

## 2. `POST /api/notifications` (recommended - your services use this)

The app doesn't call this; it's how your other software gets notifications *into* your server's storage so the `GET` above can return them. The server assigns `id` and `timestamp`.

### Request

| Part | Value |
|------|-------|
| Header `Content-Type` | `application/json` |
| Header `Authorization` | `Bearer <token>` (use the same token, or a separate write token) |
| Body | `projectKey`, `title`, `message` (all strings, required) |

```bash
curl -X POST "https://your-server.example.com/api/notifications" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "projectKey": "my-web-app",
    "title": "Deploy finished",
    "message": "v2.4.1 deployed to production in 38s"
  }'
```

### Suggested response - `201 Created`

```json
{
  "id": "a-unique-stable-id",
  "projectKey": "my-web-app",
  "title": "Deploy finished",
  "message": "v2.4.1 deployed to production in 38s",
  "timestamp": 1739203200000
}
```

Suggested errors: `400` for missing/invalid fields, `401` for a bad token.

---

## Implementation notes

- **Server-assigned `timestamp`, monotonic.** The app stores the highest `timestamp` it has seen and sends it as `since`. If two notifications can get the same millisecond, or one is written late with an older timestamp, the app can miss it. Generate timestamps on the server at insert time and make sure they strictly increase (e.g. bump by 1 ms on collision).
- **`since` is exclusive.** Use `timestamp > since`, not `>=`.
- **Order.** Return notifications oldest → newest.
- **Retention.** The app prunes its own local history after 30 days. The server only needs to keep enough history to survive the app being offline (and must still serve `since=0` sensibly).
- **Poll rate.** Default is ~20 s; the app clamps the user-configured interval to 5 s–3600 s. Keep the GET cheap (index on `timestamp`).
- **Timeouts.** The app uses 15 s connect and 15 s read timeouts. Failed polls are silently retried on the next cycle.
- **Base URL.** The user enters the base URL in the app's Settings (e.g. `https://your-server.example.com`); `/api/notifications` is appended. A trailing slash is fine.
- **CORS** is not needed (native client).

## Quick end-to-end test

```bash
# 1. Push a test notification
curl -X POST "https://your-server.example.com/api/notifications" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{"projectKey":"test","title":"Hello","message":"It works"}'

# 2. Confirm the app will see it
curl "https://your-server.example.com/api/notifications?since=0" \
  -H "Authorization: Bearer YOUR_TOKEN"

# 3. Confirm auth is enforced (expect 401)
curl -i "https://your-server.example.com/api/notifications?since=0" \
  -H "Authorization: Bearer WRONG_TOKEN"
```

Then in the app: **Settings** → enter base URL + token → enable **Background sync**; **Projects** → register key `test`. The notification should appear within one poll interval.
