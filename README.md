# ScreenStream Fork

This repository is a fork of the upstream [dkrivoruchko/ScreenStream](https://github.com/dkrivoruchko/ScreenStream) project.

It is no longer documented here as the original multi-protocol screen streaming app. The current fork has been repurposed into a single-entry Android app that opens a fixed web page in an almost full-screen `WebView`.

## What This Fork Currently Does

- App launch opens one fixed URL from `BuildConfig.LAUNCH_URL`
- The launcher activity is `singleTask`
- If the app is already running in the background, tapping the app icon again restores the existing instance instead of creating another one
- The page is shown in an edge-to-edge, near full-screen `WebView`
- System bars are hidden and can be revealed transiently by swipe
- WebView state is restored across activity recreation when possible

At the moment, the default fixed URL in source is:

```text
https://example.com/
```

See `app/build.gradle.kts` if that launch target needs to change.

## Current Product Direction

This fork has already shifted away from the upstream app's user-facing multi-mode narrative.

Current behavior is centered on:

- a single launcher entry
- a fixed web destination
- a WebView shell experience instead of the original protocol-selection product flow

Some upstream source trees still remain in the repository during the refactor, but they are not the current product surface.

## Relationship To Upstream

- Upstream project: `dkrivoruchko/ScreenStream`
- This repository: fork with local changes and different product behavior
- Upstream RTSP/WebRTC code still exists in the tree as leftover refactor residue in some directories
- Current Gradle settings only include `:app`, `:common`, and `:mjpeg`
- `rtsp` and `webrtc` are not currently included in `settings.gradle.kts`

If you want the original ScreenStream behavior and documentation, refer to the upstream repository instead of this README.

## Entry Behavior

The current launcher flow is implemented by `info.dvkr.screenstream.SingleActivity`.

Behavior summary:

1. Launch from the app icon.
2. Create or restore a single activity instance.
3. Show a black-background WebView in near full-screen mode.
4. Load the fixed launch URL on first creation.
5. If the task already exists, a new launcher tap routes to the existing activity instance via `singleTask` and `onNewIntent`.

## Build And Development

Current baseline:

- Kotlin uses `explicitApi()`
- JVM toolchain: 17
- `minSdk`: 23
- `targetSdk` / `compileSdk`: 36
- Product flavors: `FDroid`, `PlayStore`

Main verification target during the current refactor:

```bash
./gradlew :app:assembleFDroidDebug
```

Useful local commands:

```bash
./gradlew :app:testFDroidDebugUnitTest
./gradlew :app:assembleFDroidDebug
```

## CI Status

GitHub Actions currently contains one Android workflow at `.github/workflows/android-build.yml`.

That workflow currently:

- runs on pushes to `master` and `main`
- runs on pull requests and manual dispatch
- sets up JDK 21 and Android SDK 36
- runs `:app:testFDroidDebugUnitTest`
- runs `:app:assembleFDroidDebug`
- uploads the FDroid debug APK artifact

So the active CI verification is aligned with the FDroid debug build, not with the old upstream multi-protocol release story.

## Repository Notes

- `app`: current Android entry app, including the launcher activity and app wiring
- `common`: shared utilities and settings code
- `mjpeg`: still present and still included in Gradle
- `rtsp`, `webrtc`: upstream remnants currently left in the repository but not included in the active Gradle project

## License

This repository continues to inherit the upstream MIT license. See [LICENSE](LICENSE).
