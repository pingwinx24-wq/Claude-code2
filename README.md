# Shade Studio

An Android app (Kotlin, Jetpack Compose) for designing a glassmorphism Quick Settings layout, with a live
preview, drag-and-drop tile ordering, persistent profiles and optional Shizuku integration.

> **Honesty note.** Android does not allow normal apps to restyle the system Quick Settings panel.
> Shade Studio designs and previews layouts, provides a real Quick Settings tile of its own, and can
> optionally write the AOSP tile list through Shizuku. See [docs/HYPEROS_ANALYSIS.md](docs/HYPEROS_ANALYSIS.md).

## Features
- Live preview that re-renders on every change; tap to toggle/select, long-press + drag to reorder tiles
- Per tile: size (1×1 / 2×1), custom label, accent colour; add/remove from a 14-tile catalog
- Panel: columns, tile height, spacing, corner radius, tile/panel blur, tile/panel transparency, border highlight,
  active/inactive/panel colours (swatches + HSV), preview wallpaper, labels and brightness-slider toggles
- Profiles: create from templates, duplicate, rename, delete, switch, JSON import/export via clipboard
- Real `TileService` ("Shade profile") that cycles profiles from the system panel
- Shizuku (opt-in): status detection, permission flow, read / apply / restore of `sysui_qs_tiles`

## Modules
- `:core` — pure Kotlin/JVM: model, layout engine, JSON, QS-spec mapping. Unit-tested without Android.
- `:app` — Compose UI (`ui/glass`, `ui/editor`, `ui/profiles`, `ui/system`), persistence (`data`), system integration (`system`).

## Build
```
./gradlew :core:test :app:testDebugUnitTest :app:assembleDebug
```
Requires JDK 17+ and the Android SDK (platform 35). minSdk 26; real-time blur needs API 31+.
