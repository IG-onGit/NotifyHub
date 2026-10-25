# NotifyHub (Android)

A native Android app (Kotlin + Jetpack Compose) that polls **your own server**
for notifications and displays them as real Android system notifications,
with a per-project icon, accent color and name that you configure entirely
on-device. No Firebase, no third-party push service, no publishing - you
build it once and sideload the APK onto your phone.

## How it works

1. You register each piece of software ("project") inside the app: a name,
   a `projectKey`, a bundled icon, and an accent color.
2. Your own backend scripts/services push notifications into **your**
   server's own storage, tagged with that same `projectKey`.
3. The app runs a foreground service that polls your server's API on an
   interval (default 20s, configurable 5-120s) and posts any new ones as
   Android notifications, styled with that project's icon/color.
4. Notifications live both as real system notifications (clearable from the
   tray) and in the app's own history list (clear individually or all).

The app only ever makes outbound requests to **one endpoint** you control -
nothing is sent to any third party.

## API contract your server must implement

### `GET {serverBaseUrl}/api/notifications?since=<epochMillis>`

Called by the app on every poll cycle. `since` is the timestamp (epoch
milliseconds) of the newest notification the app has already stored - pass
everything strictly newer than that. On first run, or after a full clear,
`since` is `0`.

**Request header** (if you set an API token on the project's registration page in the app):
```
Authorization: Bearer <your-token>
```

**Expected response body** (200 OK, `application/json`):
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

Field notes:
- `id` - must be unique and **stable**. The app uses it to deduplicate, so
  don't reuse an id for a different notification, and don't change an id on
  retry.
- `projectKey` - must exactly match the key you typed when registering the
  project in the app. If it doesn't match any registered project, the
  notification still shows (with a default bell icon/color) rather than
  being dropped.
- `timestamp` - epoch milliseconds. Used both for display and for the
  `since` cursor on the next poll, so make sure your server returns it
  accurately and that newer notifications always have a strictly larger
  value.
- Return an empty `"notifications": []` array (not an error) when there's
  nothing new - that's a normal, expected response every poll cycle.

### How your other systems feed this endpoint

This app only ever calls the `GET` above - it never pushes to your server.
How notifications get *into* your server's storage is entirely up to you
(that's the "push" side you already said you'd build). A minimal pattern:
a small `POST /api/notifications` endpoint on your server that your other
services call, which appends to a table/queue that the `GET` above reads
from - but the app doesn't care how you implement that part.

Example of what one of your services might send to your own `POST` endpoint:
```bash
curl -X POST https://your-server.example.com/api/notifications \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <your-token>" \
  -d '{
    "projectKey": "my-web-app",
    "title": "Deploy finished",
    "message": "v2.4.1 deployed to production in 38s"
  }'
```
(Your server would assign the `id` and `timestamp` server-side.)

For a fuller backend guide (field rules, timestamp handling, test commands)
see [instruction.md](instruction.md).

## Security notes

- Use **HTTPS** for `serverBaseUrl`. The token is sent as a bearer header
  on every request, so plain HTTP would expose it on the network.
- The token is stored in `EncryptedSharedPreferences` (AES-256) and is
  excluded from Android's auto-backup, so it isn't written to cloud/ADB
  backups.
- There's no public app store listing or distribution - you build and
  install the APK directly on your own device (see below).

## Project structure

```
app/src/main/java/com/notifyhub/app/
  MainActivity.kt            Entry point, notification permission, nav host
  NotifyHubApp.kt             Application class, wires DB + settings
  data/                       Room entities/DAOs, repository, encrypted settings
  network/                    Retrofit API service + DTOs
  service/                    Foreground polling service, boot receiver
  notification/               Icon catalog, Android notification building
  ui/                          Compose screens (Notifications, Projects,
                                Add/Edit Project, Settings) + navigation + theme
```

## Building and installing on your phone

You'll need [Android Studio](https://developer.android.com/studio) (free)
installed on your computer - that's the standard, most reliable way to
build this, and it handles the Android SDK/Gradle setup for you.

1. Clone this repository and open the folder in Android Studio
   (**File → Open**). Let it sync Gradle on first open (downloads
   dependencies - needs internet once).
2. Connect your Android phone via USB, with
   **Developer options → USB debugging** enabled (Settings → About phone →
   tap "Build number" 7 times to unlock Developer options).
3. Click **Run ▶** in Android Studio with your phone selected as the
   target. It builds and installs the app directly.

   - or, to get an installable `.apk` file you can copy over and install
   manually (no cable needed after that): **Build → Build App Bundle(s) /
   APK(s) → Build APK(s)**, then copy
   `app/build/outputs/apk/debug/app-debug.apk` to your phone (e.g. via
   `adb push`, a cloud drive, or USB file transfer) and open it there to
   install. You'll need to allow "install from this source" when prompted.

4. On first launch, grant the notification permission when asked.
5. Go to **Projects** and register your first piece of software (name,
   project key, server base URL, optional API token, icon, color) - the key
   must match what your server tags notifications with. Each project has its
   own server URL and token.
6. Go to **Settings** and toggle **Background sync** on.

### Note on the Gradle wrapper

This repo includes `gradle/wrapper/gradle-wrapper.properties` (pointing at
Gradle 8.7) but not the wrapper jar/script binaries. Opening the project in Android Studio handles this
automatically. If you build from the command line instead, run
`gradle wrapper` once (with any local Gradle install) to regenerate
`gradlew` / `gradlew.bat` / `gradle-wrapper.jar`, then use `./gradlew
assembleDebug` as usual.

## License

Released under the [MIT License](LICENSE).
