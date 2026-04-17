# Repository Guidelines

## Collaboration Mode
- Use team mode for substantial work.
- The main model acts as the orchestrator: gather context, assign bounded subtasks, integrate results, and make final technical decisions.
- UI/UX implementation should be delegated to a dedicated designer sub-agent when delegation is available.
- Keep non-UI architecture, build-system changes, integration, and verification under the orchestrator unless explicitly reassigned.
- When sub-agents are used, give them narrow ownership and avoid overlapping file edits.

## Purpose & Modes
ScreenStream streams Android screen and audio.
Original upstream modes are Local (MJPEG), Global (WebRTC), and RTSP.
This workspace is being adapted toward a local-network-first MJPEG build, but upstream multi-mode structure still exists in parts of the codebase until the refactor is finished.

## Project Structure & Modules
- `app`: Compose UI shell, DI wiring, and product flavors.
- `common`: shared UI, preferences helpers, logging, utilities.
- `mjpeg`: embedded Ktor HTTP server for local MJPEG streaming.
- `rtsp`: RTSP server/client implementation and settings.
- `webrtc`: WebRTC streaming.

## Build & SDK Baseline
- Kotlin uses `explicitApi()` with JVM toolchain 17.
- SDKs: min 23, target/compile 36.
- Main verification target during the refactor is `./gradlew :app:assembleFDroidDebug`.
- Build types: release enables minify/shrink and Crashlytics mapping upload.
- Flavors: `FDroid`, `PlayStore`.

## Coding Style & Naming Conventions
- Follow official Kotlin style and keep public APIs explicit and well-typed.
- Use 4-space indentation and standard Android Studio formatting.
- Product flavors are PascalCase (`FDroid`, `PlayStore`).
- Module names are lowercase (`app`, `common`, `mjpeg`, `rtsp`, `webrtc`).

## Localization Rules
- English `values/strings.xml` is the source of truth for key order and blank lines.
- Locale files should mirror English key order and blank-line placement.
- Strings live in module-local `res/values` directories. Avoid hardcoded literals in UI code.

## Commit & Pull Request Guidelines
- Use short, sentence-style commit summaries scoped to the change.
- Keep refactor commits narrow when removing protocols or simplifying UI.

## Security & Configuration Tips
- `local.properties` may contain local Play Store configuration. Do not commit secrets.
- The checked-in debug keystore is for local development only.
- Keep WebRTC signaling endpoints configurable; never hardcode credentials.
- When debugging crashes or ANRs in PlayStore variants, use Crashlytics data before proposing fixes.
