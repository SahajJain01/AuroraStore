# Aurora Store TV Fork Guide

This document explains how this fork differs from upstream Aurora Store and how to keep the TV experience coherent over time.

## Product Positioning

Aurora Store TV Fork is a remote-first Android TV and Google TV client for browsing Google Play content through the Aurora Store foundation.

The fork is intended for:

- Android TV and Google TV boxes, sticks, projectors, and televisions.
- Users who navigate with a D-pad remote.
- Store discovery flows where TV-compatible apps should appear before phone-only apps.

The fork is not intended to be a general redesign of the upstream phone app.

## Upstream Relationship

Upstream Aurora Store remains the source of truth for the core app-store client, installers, account flows, localization, policy text, and general bug fixes.

This fork owns:

- TV-specific product flavor behavior.
- TV UI composition and layout.
- TV focus states and remote navigation.
- TV app ranking heuristics.
- TV-specific QA and release documentation.

When syncing from upstream, prefer keeping TV-specific changes isolated in files or branches that are easy to rebase and review.

## TV Build Flavor

The TV fork uses the `tv` product flavor in `app/build.gradle.kts`.

Important properties:

- `applicationIdSuffix = ".tv"`
- `versionNameSuffix = "-tv"`
- `FORCE_TV_UI = true`
- TV launcher support through `CATEGORY_LEANBACK_LAUNCHER`
- TV banner through `android:banner`

Debug package name:

```text
com.aurora.store.tv.debug
```

Build command:

```powershell
.\gradlew.bat :app:assembleTvDebug
```

Install command:

```powershell
adb install -r app\build\outputs\apk\tv\debug\app-tv-debug.apk
```

## UI Principles

Use Google's Android TV guidance as the baseline:

- Treat the D-pad as the primary input.
- Keep one obvious focused element.
- Make all visible actions reachable by remote.
- Use a 16:9 layout with overscan-safe margins.
- Keep content readable at 10 feet.
- Use dark surfaces and avoid bright white full-screen panels.
- Prefer stable list dimensions over decorative focus effects that move nearby content.
- Use passive section headers plus explicit "More" or "Show all" tiles where deeper navigation exists.

This fork intentionally avoids card enlargement on horizontal shelf cards because scaling caused row jumping and clipping on TV. Focus is indicated with container color changes instead.

## Implemented TV Surface

Primary TV implementation files:

- `app/src/main/java/com/aurora/store/compose/ui/main/TvMainScreen.kt`
- `app/src/main/java/com/aurora/store/compose/composable/TvFocus.kt`
- `app/src/main/java/com/aurora/store/util/TvAppRanker.kt`
- `app/src/main/java/com/aurora/store/compose/ui/details/AppDetailsScreen.kt`
- `app/src/main/java/com/aurora/store/compose/ui/search/SearchScreen.kt`

TV behavior is selected in `ComposeActivity` when either:

- `BuildConfig.FORCE_TV_UI` is true, or
- the device reports as TV through `PackageUtil.isTv(context)`.

## TV App Ranking

TV-first ordering is applied through `tvOptimizedFirst()` across:

- Home streams.
- Browse streams.
- Expanded browse pages.
- Category streams.
- Top charts.
- Search results.
- App details suggestions.
- Developer profile lists.

The ranker uses visible Play metadata signals such as package names, app text, badges, compatibility data, category terms, and TV-oriented wording.

The ranker is heuristic. Treat it as a prioritization layer, not a certification system.

## QA Checklist

Before publishing a TV release, verify at minimum:

- App appears in a TV launcher through the Leanback launcher entry.
- Initial focus lands on a usable element.
- D-pad Up, Down, Left, Right, Center, and Back work on every primary screen.
- Left rail expands and collapses predictably.
- Home shelves do not jump or clip when moving horizontally.
- Section headers are not focus traps.
- "More" tiles are reachable at the end of shelves.
- Search moves from query entry to results without requiring touch or mouse input.
- App details opens with the primary action focused.
- Details content stays within overscan-safe margins.
- No white full-screen panels appear in TV mode.
- TV-optimized apps appear first in search and top charts for common TV queries.
- Back returns to the previous screen or exits predictably.

Recommended commands:

```powershell
.\gradlew.bat :app:compileTvDebugKotlin :app:ktlintCheck :app:assembleTvDebug
adb install -r app\build\outputs\apk\tv\debug\app-tv-debug.apk
adb shell am start -n com.aurora.store.tv.debug/com.aurora.store.ComposeActivity
```

For visual evidence:

```powershell
adb shell screencap -p /sdcard/tv-home.png
adb pull /sdcard/tv-home.png .
adb shell uiautomator dump /sdcard/tv-ui.xml
adb pull /sdcard/tv-ui.xml .
```

## Current Known Risks

- Debug builds on the Android 9 TV emulator still show high jank in `dumpsys gfxinfo`.
- Image-heavy rows and details pages can cause long frames while content loads.
- Search and stream data quality depends on Google Play metadata.
- The fork still inherits non-TV flows from upstream that may need more remote-specific polish over time.

## Release Readiness Bar

A release should not be called polished until it has been tested on real Android TV hardware, not only an emulator.

Minimum release evidence should include:

- Screenshots from home, search, details, updates, and settings.
- `dumpsys gfxinfo` or profiler results from a release build.
- Manual D-pad QA notes.
- Confirmation that app listings and banners are correct for the TV launcher.
- Clear release notes in `FORK_CHANGELOG.md`.
