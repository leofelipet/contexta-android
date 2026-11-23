# AGENTS.md

Native Android client (Kotlin and Jetpack Compose) for the Contexta backend. It talks **directly to the Go API** with `API_BEARER_TOKEN`, not through the web.

## Contexta: three repos

Contexta lives in three sibling repos under the same folder:

- `contexta-backend`: Go API, MCP server and Postgres migrations.
- `contexta-web`: Next.js admin panel, which calls the API.
- `contexta-android`: Kotlin app, which calls the API directly.

Features ship as paired commits with the same version: `internal/version` in the backend and `package.json` in the web. Android has its own `versionName`; bump its `versionCode` too, so the in-app update sees the new version.

**Push order:** backend first, then web, then Android. The web and the app depend on API changes such as migrations and new fields.

**Verification before each push:**

- Backend: `go build ./... && go vet ./... && go test ./...`. Integration tests need `TEST_DATABASE_URL`.
- Web: `pnpm typecheck && pnpm lint && pnpm test && pnpm build`.
- Android: **do not run `./gradlew` on the local machine.** Gradle plus the Kotlin daemon use 2 to 3 GB of RAM and freeze the computer. Push, and let GitHub Actions verify: the `APK` workflow runs `test assembleRelease` and publishes the release. Follow it with `gh run watch <id> -R leofelipet/contexta-android --exit-status`.

Do not run heavy builds in parallel. When reporting a result, use the command's real exit code: `cmd | tail; echo $?` reports the exit code of `tail`.

## Public repo

- This repository is public. Never commit server URLs, tokens or keystores.
- `*.jks` and `local.properties` are gitignored. The user pastes the API URL and token in the app on first launch.

## Structure

- It is a single-activity app. The screens are composables in `MainActivity.kt`.
- `Api.kt`: OkHttp calls and SSE. `Models.kt`: kotlinx.serialization models. `Cache.kt`: in-memory session cache. `Prefs.kt`: settings. `EventsService.kt`: SSE listener. `Alarms.kt` / `ReminderReceiver.kt` / `Notify.kt`: local reminders.
- JSON is decoded with `contextaJson` (`ignoreUnknownKeys`, `explicitNulls = false`). Give every model field a default value, so that new or missing API fields do not crash the app.
- When the API changes the **shape** of a field (for example, a string that becomes an object), update `Models.kt`, the screen that uses it, and `ParseTest.kt` in the same commit. CI runs those tests.
- All UI text is in Brazilian Portuguese.
- `ROADMAP.md` defines the scope. The app does not clone the web 1:1: passkeys, charts, the email client, WhatsApp sending and FCM stay out.

## Releases

- Bump `versionCode` and `versionName` in `app/build.gradle.kts`. The in-app update compares the `versionCode`.
- Every push to `main` runs the `APK` workflow: tests, a signed `assembleRelease`, and a GitHub release `v<versionName>`. The signing keystore comes from repository secrets.
- The phone updates from the Go server, not from GitHub: the APK and `latest.json` go into `ANDROID_DIST_DIR`, and their `version_code` must match the APK.
- For commits that only touch docs, add `[skip ci]` to the commit message so they do not publish a release.
