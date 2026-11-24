# RAM Overlay Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Android app that draws a `RAM NN%` pill over the quick-settings chip while the notification panel is open.

**Architecture:** An `AccessibilityService` adds one view with `TYPE_ACCESSIBILITY_OVERLAY`, refreshed by a 2 s handler loop and shown or hidden from window-change events. Pure Kotlin helpers hold the logic so JVM unit tests cover it. A launcher activity handles setup and the always-show switch.

**Tech Stack:** Kotlin 1.9.24, AGP 8.5.2, Gradle 8.7, compileSdk/targetSdk 34, minSdk 26, JUnit4, GitHub Actions.

**Spec:** docs/superpowers/specs/2026-11-25-ram-overlay-design.md

## Global Constraints
- Package `com.ramoverlay.app`; app label `RAM Overlay`.
- Overlay window type `TYPE_ACCESSIBILITY_OVERLAY`; flags `FLAG_NOT_FOCUSABLE | FLAG_NOT_TOUCH_MODAL | FLAG_LAYOUT_NO_LIMITS`, plus `FLAG_NOT_TOUCHABLE` except while calibrating.
- Refresh interval 2000 ms. Text format `RAM NN%`.
- Pill is opaque dark (`#FF111111`), white text.
- No third-party libraries; no network permission.

## Review Focus
- total memory 0 or avail > total: percent clamps to 0..100 (Task 1 test).
- No systemui window or an unknown ROM layout: shade reads as closed, and the always-show switch is the fallback (Task 2 test).
- Saved bounds larger than the screen after a display-size or rotation change: clamp inside the screen (Task 3 test).
- Landscape: hide the pill unless always-show is on (Task 4).
- Service killed by the system: setup screen shows its enabled state on resume, with a battery-optimization hint (Task 5).

---

### Task 1: Project skeleton, RAM percent, CI
**Files:**
- Create: `settings.gradle.kts`, `build.gradle.kts`, `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`, `.github/workflows/build.yml`
- Create: `app/src/main/java/com/ramoverlay/app/RamReader.kt`
- Test: `app/src/test/java/com/ramoverlay/app/RamReaderTest.kt`

**Interfaces:**
- Produces: `fun percentUsed(total: Long, avail: Long): Int` and `fun readRamPercent(ctx: Context): Int` (top-level, `RamReader.kt`).

- [ ] **Step 1: Write failing tests** in `RamReaderTest`: `percentUsed(8000, 2000) == 75`; `percentUsed(0, 0) == 0`; `percentUsed(1000, 2000) == 0`; `percentUsed(1000, -5) == 100`.
- [ ] **Step 2: Implement** `percentUsed` (round to nearest, clamp 0..100) and `readRamPercent` (via `ActivityManager.MemoryInfo.totalMem/availMem`).
- [ ] **Step 3: Gradle files and workflow.** Workflow on push: `actions/setup-java` 17, `gradle/actions/setup-gradle` with `gradle-version: 8.7`, run `gradle testDebugUnitTest assembleDebug`, upload `app/build/outputs/apk/debug/*.apk` as an artifact.
- [ ] **Step 4: Push the `ram-overlay` branch; expect a green run.**

### Task 2: Panel-open detection
**Files:**
- Create: `app/src/main/java/com/ramoverlay/app/ShadeDetector.kt`
- Test: `app/src/test/java/com/ramoverlay/app/ShadeDetectorTest.kt`

**Interfaces:**
- Produces: `data class WinInfo(val pkg: String?, val top: Int, val bottom: Int)` and `fun isShadeOpen(wins: List<WinInfo>, screenH: Int): Boolean`.

- [ ] **Step 1: Write failing tests.** A systemui window of height at least 40% of screenH gives true. A systemui window of 5% (status bar only) gives false. A large window from another package gives false. An empty list gives false.
- [ ] **Step 2: Implement.**
- [ ] **Step 3: Push; expect green.**

### Task 3: Settings and bounds clamping
**Files:**
- Create: `app/src/main/java/com/ramoverlay/app/OverlayPrefs.kt`
- Test: `app/src/test/java/com/ramoverlay/app/OverlayPrefsTest.kt`

**Interfaces:**
- Produces: `data class Bounds(val x: Int, val y: Int, val w: Int, val h: Int)`, `fun clampBounds(b: Bounds, screenW: Int, screenH: Int, minSize: Int = 48): Bounds`, and `class OverlayPrefs(ctx: Context)` with `var bounds: Bounds`, `var alwaysShow: Boolean`. Default bounds: x 60% of the screen width, y 28% of the screen height, w 160 dp, h 40 dp.

- [ ] **Step 1: Write failing tests** for `clampBounds`: a box wider than the screen shrinks to the screen width; negative x becomes 0; a box past the bottom edge is moved up; w/h below `minSize` is raised to `minSize`.
- [ ] **Step 2: Implement** `clampBounds` and `OverlayPrefs` (SharedPreferences).
- [ ] **Step 3: Push; expect green.**

### Task 4: Accessibility overlay service
**Files:**
- Create: `app/src/main/java/com/ramoverlay/app/OverlayService.kt`, `app/src/main/res/xml/accessibility_config.xml`
- Modify: `AndroidManifest.xml` (service entry with `BIND_ACCESSIBILITY_SERVICE`)

**Interfaces:**
- Consumes: `readRamPercent`, `isShadeOpen`, `OverlayPrefs`, `clampBounds`.
- Produces: `class OverlayService : AccessibilityService` with a companion `fun setCalibrating(on: Boolean)` and `fun isRunning(): Boolean`.

- [ ] **Step 1: Config.** Listen for `typeWindowsChanged|typeWindowStateChanged`, flag `flagRetrieveInteractiveWindows`, no feedback.
- [ ] **Step 2: Pill view.** Rounded dark `TextView` showing `RAM NN%`, bounds from prefs clamped to the current display. Hide unless the shade is open or `alwaysShow`; also hide in landscape unless `alwaysShow`.
- [ ] **Step 3: Calibration mode.** Touchable. Dragging the body moves it, dragging the bottom-right handle resizes it, tapping the check mark saves through `OverlayPrefs` and exits.
- [ ] **Step 4: Push; expect a green build.**

### Task 5: Setup screen and delivery
**Files:**
- Create: `MainActivity.kt`, `res/layout/activity_main.xml`
- Modify: `AndroidManifest.xml` (launcher activity)

- [ ] **Step 1: Screen.** Shows the service status (on resume), a button opening `Settings.ACTION_ACCESSIBILITY_SETTINGS`, an "Always show" switch, a "Calibrate position" button, and a short battery-optimization hint.
- [ ] **Step 2: Push; download the debug APK artifact from the green run and send it to the user in chat.**
- [ ] **Step 3: Tell the user the install steps** (allow install from this source, then Settings > Accessibility > RAM Overlay > on), and that the panel-open detection is untested on their phone, so "Always show" is the fallback.

## Self-Review
All spec goals map to Tasks 1 to 5. Names and signatures are consistent across tasks. Tests run only in CI because the workspace has no Android SDK.
