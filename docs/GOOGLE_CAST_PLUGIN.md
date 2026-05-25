# Google Cast Plugin Project

This is the single entry point for the BiliPai Google Cast plugin work. Keep it concise, update it after each completed slice, and remove stale details instead of adding parallel notes.

If `docs/GOOGLE_CAST_PLUGIN.local.md` exists, read it after this file for machine-specific environment notes. That local file is intentionally ignored and must not be committed.

## Working Rules

- Build this as a BiliPai source-level native Kotlin plugin.
- Use sub-agent driven development: architecture, task slicing, review, and verification stay with the controller; coding slices are implemented through `claude -p`.
- After every completed slice, update this file so another worker can resume quickly.
- Keep documentation progressive: this file is the only project entry. Link to existing repo docs only when needed.
- Avoid unnecessary files. Delete obsolete notes and update stale information immediately.
- Use `D:\Temp` for temporary scratch data if needed; avoid writing to `C:` unless a tool requires it.
- Do not work directly on `main`; use the `feature/google-cast-plugin` worktree at `D:\DATA\Codes\BiliPai\worktrees\google-cast-plugin`.
- Branch names, commits, PR titles, and PR bodies must use normal human engineering wording without tool or authorship labels.

## Existing Context

- Native plugins are registered in `app/src/main/java/com/android/purebilibili/app/PureApplication.kt`.
- The plugin framework lives in `app/src/main/java/com/android/purebilibili/core/plugin/`.
- Built-in plugins live in `app/src/main/java/com/android/purebilibili/feature/plugin/`.
- Existing DLNA casting lives in `app/src/main/java/com/android/purebilibili/feature/cast/`.
- Existing player cast UI is wired in `app/src/main/java/com/android/purebilibili/feature/video/ui/overlay/VideoPlayerOverlay.kt`.
- Existing BiliBili TV cast URL policy is in `app/src/main/java/com/android/purebilibili/data/repository/VideoCastPolicy.kt`.

## Architecture Decision

Implement Google Cast as a native plugin plus a focused Cast integration layer:

- Add a native plugin entry so the feature appears in the plugin center and can be enabled or disabled.
- Add a small cast-provider abstraction so the player overlay can offer DLNA and Google Cast without mixing protocol details into UI code.
- Use Google Cast CAF for Chromecast discovery/session/media loading, and reuse the current BiliBili TV cast URL fallback path where possible.
- Keep DLNA behavior intact and avoid replacing the existing DLNA manager in this project.

Official references checked on 2026-05-26:

- Google Cast Android sender setup recommends `play-services-cast-framework:22.3.1` and a Cast options provider.
- AndroidX MediaRouter latest stable is `androidx.mediarouter:mediarouter:1.8.1`.
- Media3 CastPlayer docs exist, but this slice starts with CAF session/media loading to avoid a broad player-service rewrite.

## Slices

### Slice 0: Baseline And Scaffolding

Goal: confirm the worktree, baseline focused tests, and add this documentation entry.

Status: complete.

Verification target:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "*Cast*"
```

Result on 2026-05-26: passed with `BUILD SUCCESSFUL`.

### Slice 1: Google Cast Plugin Shell

Goal: add build/manifest wiring, a native plugin class, and small policy tests proving enablement metadata and route visibility behavior.

Status: complete.

Likely files:

- `app/build.gradle.kts`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/android/purebilibili/app/PureApplication.kt`
- `app/src/main/java/com/android/purebilibili/feature/plugin/GoogleCastPlugin.kt`
- New focused tests under `app/src/test/java/com/android/purebilibili/feature/cast/`

Result on 2026-05-26:

- Added the native `GoogleCastPlugin` shell and registered it as an eighth built-in plugin.
- Added Google Cast CAF and MediaRouter dependencies.
- Added the Cast options provider manifest metadata and default receiver policy.
- Verified with `.\gradlew.bat :app:testDebugUnitTest --tests "*GoogleCast*" --no-daemon`.
- Verified with `.\gradlew.bat :app:testDebugUnitTest --tests "*Cast*" --no-daemon`.

### Slice 2: Chromecast Discovery And Selection

Goal: discover Google Cast routes, present them alongside existing DLNA entries, and honor plugin enabled state.

UI requirement: Google Cast device rows must match the existing DLNA cast list layout, spacing, selection behavior, dialog structure, and loading/empty states. Only the route-specific icon and concise Google Cast label should differ.

Status: complete.

Likely files:

- New Google Cast manager/provider files under `feature/cast/`
- `DeviceListDialog.kt`
- Focused discovery/presentation policy tests

Result on 2026-05-26:

- Added MediaRouter-based Google Cast route discovery gated by the `google_cast` plugin enabled state.
- Added route filtering/presentation policy tests for disabled plugin state, default/Bluetooth routes, cast-category support, fallback names, and deduplication.
- Added Google Cast routes to the existing device dialog using the same DLNA row structure and interaction pattern; only the icon and Google Cast label differ.
- Left Chromecast media loading as Slice 3.
- Verified with `.\gradlew.bat :app:testDebugUnitTest --tests "*GoogleCast*" --no-daemon`.
- Verified with `.\gradlew.bat :app:testDebugUnitTest --tests "*Cast*" --no-daemon`.

### Slice 3: Chromecast Media Load

Goal: when a Chromecast device is selected, resolve the current playable cast URL and load it through the active Google Cast session.

Likely files:

- Google Cast session/media client wrapper
- `VideoPlayerOverlay.kt`
- Focused tests for media metadata/request construction and URL fallback decisions

### Slice 4: Review, Verification, And Cleanup

Goal: run targeted tests and compile checks, remove dead notes/files, update this document, and prepare commits.

Verification ladder:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "*Cast*"
.\gradlew.bat :app:compileDebugKotlin
```

## Progress Log

- 2026-05-26: Created isolated worktree `feature/google-cast-plugin`.
- 2026-05-26: Confirmed existing casting is DLNA/SSDP based; Google Cast/Chromecast support is not implemented yet.
- 2026-05-26: Completed Slice 0 documentation and focused Cast baseline.
- 2026-05-26: Completed Slice 1 Google Cast plugin shell, CAF wiring, and focused policy tests.
- 2026-05-26: Completed Slice 2 Chromecast route discovery and device-list selection UI aligned with existing DLNA rows.
