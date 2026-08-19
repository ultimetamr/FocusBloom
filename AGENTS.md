# Focus Bloom project guidance

This directory is the isolated source of truth for Focus Bloom. Do not edit sibling projects when working on this app.

Focus Bloom is a PICO OS 6 Shared Space planar spatial pomodoro MVP. Its package is `com.pico.swan.focusbloom`, and the launcher is `.platform.LaunchActivity`.

Key areas:

- `app/src/main/java/com/pico/swan/focusbloom/domain/` — timer state machine, focus models, and drop-target rules.
- `app/src/main/java/com/pico/swan/focusbloom/data/` — local SharedPreferences persistence.
- `app/src/main/java/com/pico/swan/focusbloom/ui/` — unidirectional screen state and all six product surfaces.
- `app/src/main/java/com/pico/swan/focusbloom/platform/` — Spatial Application, launch Activity, and screenshot export.
- `app/src/test/` — pause/resume, cross-date recovery, growth, formatting, and hit-target tests.

All 2D UI must use SpatialUI and remain wrapped in `PicoTheme`; Material/Material3 is forbidden. Preserve the planar `DefaultWindowContainer` Shared Space architecture and its controller/click fallback for spatial drag interactions.

Build and verify from this directory only:

```text
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
pico-cli app install app/build/outputs/apk/debug/app-debug.apk
pico-cli app launch com.pico.swan.focusbloom --activity .platform.LaunchActivity
```

Simulator screenshots and logs belong under `artifacts/` in this directory.

<!-- pico-cli:plugin-context:pico-spatial-agentic-tools:start -->
## Plugin Context

Also read `./PICO-SPATIAL-AGENTIC-TOOLS.AGENTS.md` for PICO Spatial plugin guidance.
<!-- pico-cli:plugin-context:pico-spatial-agentic-tools:end -->
