# Technical limits: Xiaomi HyperOS on Redmi Note 14 4G

Scope of this note: what a normal, Play-Store-style app can and cannot do to the Quick Settings /
Control Center on this device, and which parts of Shade Studio depend on that. **Nothing here has been
verified on a physical Redmi Note 14 4G** — the project was developed in a headless container with no
device. Statements about HyperOS behaviour are based on how AOSP and Xiaomi's skin are generally
documented and are marked *unverified* where relevant.

## 1. The system panel is not restylable by apps
- Quick Settings / Control Center is drawn by `com.android.systemui`, a platform-signed privileged process.
  No public API lets another app change its blur, tile geometry, colours or layout.
- Changing that would require root, a Magisk/LSPosed module or a rebuilt ROM. Those are out of scope; Shade
  Studio does not attempt them and does not pretend to.
- Shizuku (shell UID 2000) does **not** change this: shell has no right to modify another app's UI, only to
  run shell commands and write the subset of settings the shell UID may write.

## 2. HyperOS specifics (all *unverified* on-device)
- HyperOS (like MIUI 14) can use a split layout: notification shade and a separate Control Center. Whether it
  reads the AOSP `secure:sysui_qs_tiles` list, a private list, or both, is not something this project has
  confirmed. The Shizuku "Apply" action is therefore labelled experimental, backs up the previous value first,
  and has a one-tap restore.
- Xiaomi gates "USB debugging (Security settings)" separately from normal USB debugging; ADB-based Shizuku start
  or `pm grant` flows may need it enabled. Wireless-debugging start also requires the device to be on Wi-Fi.
- Aggressive background management (Battery saver "Restrict" mode, no Autostart) can kill Shizuku's service after
  a reboot; it must be restarted manually. Shade Studio's own tile service is bound by the system on demand and
  does not need Autostart.
- Public specifications list this model as shipping with Android 14 and a HyperOS 1.x build, i.e. API 34. The
  app reads the real values at runtime (System tab) instead of assuming them.

## 3. What Shade Studio does with these constraints

| Capability | Mechanism | Needs | Status |
|---|---|---|---|
| Design & live-preview a glass QS panel | In-app Compose preview | nothing | implemented |
| Real-time blur in preview | `RenderEffect` / `GraphicsLayer` | Android 12+ (API 31) | implemented; below API 31 falls back to tint only |
| Persistent profiles, import/export | SharedPreferences + JSON | nothing | implemented |
| A real system QS tile ("Shade profile") | `TileService` | user adds tile | implemented |
| Prompt to add that tile | `StatusBarManager.requestAddTileService` | Android 13+ | implemented; HyperOS may decline, then manual add |
| Write tile list to system | `settings put secure sysui_qs_tiles` via Shizuku | Shizuku running + permission | implemented, **effect on HyperOS unverified** |
| Restyle the real panel (blur/colour/size) | — | root/Xposed | **not possible, not implemented** |

## 4. Security notes on the Shizuku path
- Only two commands are ever issued: `settings get|put|delete secure sysui_qs_tiles`.
- The value written is built from a fixed catalog of tile specs and must match `^[A-Za-z0-9_().:-]+(,…)*$`
  (`QsSpec.isSafe`, unit-tested against injection) before it reaches `sh -c`.
- `Shizuku.newProcess` is private in Shizuku API 13, so it is called by reflection. This is the one
  fragile point; a `UserService` is the long-term replacement.
