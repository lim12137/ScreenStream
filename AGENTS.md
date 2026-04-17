# Repository Guidelines

## Collaboration Mode
- Use team mode for substantial work.
- The main model acts only as the orchestrator: gather context, assign bounded subtasks, integrate results, and make final technical decisions.
- The main model should avoid doing implementation work directly when a bounded sub-agent can own it safely.
- Default rule for this repository: concrete coding, UI work, docs edits, cleanup, verification, and research should be delegated to sub-agents whenever possible.
- UI/UX implementation should be delegated to a dedicated designer sub-agent when delegation is available.
- Keep only orchestration, prioritization, review, and final judgment in the main model unless the user explicitly asks otherwise.
- When sub-agents are used, give them narrow ownership and avoid overlapping file edits.

## Purpose & Modes
ScreenStream streams Android screen and audio.
Upstream originally supported Local (MJPEG), Global (WebRTC), and RTSP.
This workspace is now being actively narrowed to a local-network-first MJPEG build.
Treat the product target as single-mode local streaming even if some upstream RTSP/WebRTC source trees still remain in the repository during cleanup.

## Project Structure & Modules
- `app`: Compose UI shell, DI wiring, and product flavors.
- `common`: shared UI, preferences helpers, logging, utilities.
- `mjpeg`: embedded Ktor HTTP server for local MJPEG streaming.
- `rtsp`: upstream RTSP implementation retained as cleanup residue unless explicitly removed.
- `webrtc`: upstream WebRTC implementation retained as cleanup residue unless explicitly removed.

## Build & SDK Baseline
- Kotlin uses `explicitApi()` with JVM toolchain 17.
- SDKs: min 23, target/compile 36.
- Main verification targets are `./gradlew :app:testFDroidDebugUnitTest` and `./gradlew :app:assembleFDroidDebug`.
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
- Keep refactor commits narrow when removing upstream protocol paths or simplifying UI.

## Security & Configuration Tips
- `local.properties` may contain local Play Store configuration. Do not commit secrets.
- The checked-in debug keystore is for local development only.
- Do not reintroduce external signaling or cloud relay assumptions unless explicitly requested.
- When debugging crashes or ANRs in PlayStore variants, use Crashlytics data before proposing fixes.
