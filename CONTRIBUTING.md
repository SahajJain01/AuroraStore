# Contributing To Aurora Store TV Fork

Thanks for helping improve this TV-focused fork. The goal is a reliable, remote-first app store experience for Android TV and Google TV.

## Contribution Focus

Good contributions usually improve one of these areas:

- D-pad navigation and focus behavior.
- TV-safe layouts, spacing, typography, and dark theme polish.
- Performance on TV hardware.
- TV app ranking and discovery.
- Installer, account, or update fixes inherited from upstream Aurora Store.
- Documentation, QA notes, and release process.

Avoid broad redesigns that do not improve the TV use case.

## Development Setup

Use JDK 21 and a recent Android SDK.

Build the TV debug APK:

```powershell
.\gradlew.bat :app:assembleTvDebug
```

Run the main quality checks:

```powershell
.\gradlew.bat :app:compileTvDebugKotlin :app:ktlintCheck :app:assembleTvDebug
```

Install on a connected TV device or emulator:

```powershell
adb install -r app\build\outputs\apk\tv\debug\app-tv-debug.apk
```

## TV UI Rules

- Every primary workflow must work with a D-pad remote.
- Do not rely on touch, mouse hover, or the Android Menu key.
- Keep focus states obvious and stable.
- Do not enlarge horizontal shelf cards if it causes row jumping or clipping.
- Prefer passive headers with explicit "More" tiles for deeper navigation.
- Keep TV screens dark unless a content asset itself is bright.
- Keep important UI inside overscan-safe margins.
- Make list keys stable so Compose can preserve item identity.

## Testing Expectations

For UI changes, include notes about:

- Device or emulator used.
- Android TV version if known.
- Screens tested.
- D-pad path tested.
- Any remaining jank, clipping, or focus oddities.

At minimum, run:

```powershell
.\gradlew.bat :app:compileTvDebugKotlin :app:ktlintCheck :app:assembleTvDebug
```

For performance-sensitive changes, also collect:

```powershell
adb shell dumpsys gfxinfo com.aurora.store.tv.debug
```

## Working With Upstream

Keep upstream Aurora Store changes separate from TV-specific work when possible.

Recommended branch types:

- `sync/upstream-YYYY-MM-DD` for upstream syncs.
- `tv/focus-*` for focus or navigation work.
- `tv/perf-*` for TV performance work.
- `docs/*` for documentation.

When upstream changes conflict with TV files, prefer preserving remote usability over mobile layout assumptions in the TV flavor.

## Documentation

Update docs when behavior changes:

- `README.md` for high-level fork identity and build instructions.
- `TV_FORK.md` for TV architecture, QA, and release expectations.
- `FORK_CHANGELOG.md` for fork-specific release notes.

Do not remove upstream legal, license, or disclaimer context.
