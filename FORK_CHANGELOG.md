# Aurora Store TV Fork Changelog

This file tracks changes specific to the TV-focused fork. The inherited upstream Aurora Store changelog remains in `CHANGELOG`.

## Unreleased

- Added a dedicated Android TV product flavor with forced TV UI behavior.
- Added Leanback launcher support and TV launcher banner metadata.
- Rebuilt the main store surface as a TV-first interface with a collapsible left rail, large hero content, horizontal shelves, and D-pad navigation.
- Added dark TV theme behavior for lower-glare use on large screens.
- Added TV focus helpers for stable color-based focus states.
- Removed horizontal shelf card focus scaling to prevent row jumping and bottom clipping.
- Added end-of-row "More" tiles so section headers can remain passive.
- Added TV-first app ranking and applied it across home streams, browse pages, categories, top charts, search, developer pages, and details suggestions.
- Reworked app details for TV with single-pane layout, large action buttons, initial primary-action focus, screenshot rows, and TV-safe spacing.
- Improved search for TV with single-pane results, stable list keys, TV-first result ordering, and first-result focus after search.
- Added TV-specific docs, contribution guidance, and release readiness notes.

## Verification Notes

- Built with `:app:compileTvDebugKotlin`, `:app:ktlintCheck`, and `:app:assembleTvDebug`.
- Installed and tested on an Android 9 TV emulator.
- Verified D-pad navigation through the rail, home shelves, "More" tile, details page, developer focus, and search results.
- Debug emulator profiling still reports jank during startup and image-heavy navigation; release profiling on real TV hardware remains required.
