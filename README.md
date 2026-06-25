# Aurora Store TV Fork

> A TV-first fork of Aurora Store focused on Android TV and Google TV devices.

This repository is a fork of the original [Aurora Store](https://github.com/whyorean/AuroraStore). The fork keeps Aurora Store's Google Play client foundation, but reshapes the application around a 10-foot TV experience: D-pad navigation, large readable layouts, a dark living-room theme, TV-safe spacing, and TV-optimized app discovery.

## Why This Fork Exists

The upstream Aurora Store app works well on phones and tablets, but on many Android TV devices it behaves like a mobile app rotated into landscape. That makes it hard to use with a remote, especially for browsing rows, opening app details, searching, and installing apps from the couch.

This fork exists to make Aurora Store practical on TVs.

## Fork Goals

- Make the TV UI the primary experience for Android TV and Google TV.
- Keep every important action reachable with a D-pad remote.
- Prioritize TV-optimized apps in home streams, search results, categories, developer pages, suggestions, and top charts.
- Use a dark, low-glare visual system suitable for large screens.
- Follow Google's TV design guidance for focus, layout, overscan-safe spacing, and remote-first navigation.
- Preserve the upstream Aurora Store foundation where it makes sense, while allowing this fork to move independently for TV-specific UX.

## Current TV Highlights

- Dedicated `tv` product flavor with `FORCE_TV_UI=true`.
- Leanback launcher support and a TV banner.
- Collapsible left navigation rail for Search, Apps, Games, Updates, Downloads, and Settings.
- TV-specific home screen with hero content, horizontal shelves, passive section headers, and reachable "More" tiles.
- Fixed-size TV cards with color-based focus states to avoid row jumping.
- TV-optimized ranking for app lists, search, categories, top charts, developer pages, and suggestions.
- TV-friendly app details page with a single-pane layout, large actions, screenshot row, dark background, and initial action focus.
- TV search flow with single-pane results, stable list keys, and first-result focus handoff after search.

See [TV_FORK.md](TV_FORK.md) for implementation scope, quality status, known limitations, and maintenance notes.

## Relationship To Upstream

This is an independent fork. It is not the official Aurora Store project, and it is not affiliated with AuroraOSS, Google, Google Play, or any app developers.

The fork should periodically pull security, API, installer, translation, and bug-fix work from upstream Aurora Store. TV UX changes should remain focused in this fork unless they are generally useful upstream.

## Build The TV App

Prerequisites:

- JDK 21
- Android SDK with the compile SDK required by the project
- Android platform tools if you want to install with `adb`

Build a debug TV APK:

```powershell
$env:ANDROID_HOME='C:\Android\Sdk'
$env:ANDROID_SDK_ROOT='C:\Android\Sdk'
.\gradlew.bat :app:assembleTvDebug
```

The APK is written to:

```text
app/build/outputs/apk/tv/debug/app-tv-debug.apk
```

Install it on a connected Android TV device or emulator:

```powershell
adb install -r app\build\outputs\apk\tv\debug\app-tv-debug.apk
```

Release builds require the normal Android signing setup used by the upstream project.

## Quality Status

The TV UI has been tested on an Android 9 TV emulator for:

- Launcher entry and TV startup.
- D-pad navigation across the left rail, hero card, shelves, and "More" tiles.
- Horizontal shelf stability with no card enlargement on focus.
- App details layout, action focus, developer-link focus, and screenshot browsing.
- Search result ranking and focus handoff.

Known caveat: debug/emulator profiling still shows jank during cold startup, app-details load, and row navigation. The UI structure is TV-first, but release profiling on real Android TV hardware is still required before calling the experience fully polished.

## What Aurora Store Does

Aurora Store enables users to search and download apps from the official Google Play store. Users can view app descriptions, screenshots, updates, reviews, and download APKs directly from Google Play to their device.

Unlike a traditional app store, Aurora Store does not own, license, or distribute any apps. Apps, app descriptions, screenshots, and other content shown by Aurora Store are accessed from Google Play.

Please read [DISCLAIMER.md](DISCLAIMER.md) before using this fork with a personal Google account.

## Features Inherited From Aurora Store

- GPLv3-or-later free software.
- Personal or anonymous account login.
- Device and locale spoofing.
- Exodus Privacy integration.
- Plexus compatibility information.
- Update blacklisting.
- Download manager.
- Manual downloads for available APK versions.

## Limitations

- The underlying Google Play API is reverse engineered and can break when Google changes behavior.
- Paid apps cannot be downloaded or updated through anonymous accounts.
- Apps and games using Play Asset Delivery may not update correctly.
- Some features are unavailable when logged in anonymously, including library, purchase history, beta programs, and reviews.
- Token dispenser downtime can affect anonymous login.
- TV performance still needs real-device profiling and optimization.

## Downloads

This fork does not currently publish an official stable TV release channel. Build the `tv` flavor from source for testing.

For the official upstream Aurora Store project, use the upstream release sources:

- [AuroraOSS website](https://auroraoss.com/)
- [GitLab Releases](https://gitlab.com/AuroraOSS/AuroraStore/-/releases)
- [F-Droid](https://f-droid.org/packages/com.aurora.store/)
- [IzzyOnDroid](https://apt.izzysoft.de/fdroid/index/apk/com.aurora.store)

Those upstream builds are not this TV-focused fork.

## Permissions

- `android.permission.INTERNET` to download and install or update apps from Google Play servers.
- `android.permission.ACCESS_NETWORK_STATE` to check internet availability.
- `android.permission.FOREGROUND_SERVICE` and `android.permission.FOREGROUND_SERVICE_DATA_SYNC` to download apps without interruption.
- `android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` to support uninterrupted automatic updates.
- `android.permission.MANAGE_EXTERNAL_STORAGE`, `READ_EXTERNAL_STORAGE`, and `WRITE_EXTERNAL_STORAGE` for OBB expansion files.
- `android.permission.QUERY_ALL_PACKAGES` to check updates for installed apps.
- `android.permission.REQUEST_INSTALL_PACKAGES` and `REQUEST_DELETE_PACKAGES` to install, update, and uninstall apps.
- `android.permission.ENFORCE_UPDATE_OWNERSHIP` and `UPDATE_PACKAGES_WITHOUT_USER_ACTION` for silent update flows where supported.
- `android.permission.POST_NOTIFICATIONS` for download, update, and error notifications.
- `android.permission.USE_CREDENTIALS` to allow personal Google account sign-in through microG.

## Contributing

This fork welcomes changes that improve Android TV usability, performance, accessibility, and maintainability. Start with [CONTRIBUTING.md](CONTRIBUTING.md).

High-value contribution areas:

- Reducing Compose jank and cold-start cost on real TV hardware.
- Improving D-pad focus behavior and edge cases.
- Adding TV screenshots and release metadata.
- Tightening TV app ranking signals.
- Keeping the fork current with upstream Aurora Store fixes.

## License

Aurora Store is licensed under GPL-3.0-or-later. This fork keeps the same license. See [LICENSE](LICENSE) and [LICENSES](LICENSES).

## Project References

Aurora Store is based on these projects:

- [YalpStore](https://github.com/yeriomin/YalpStore)
- [AppCrawler](https://github.com/Akdeniz/google-play-crawler)
- [Raccoon](https://github.com/onyxbits/raccoon4)
- [SAI](https://github.com/Aefyr/SAI)
