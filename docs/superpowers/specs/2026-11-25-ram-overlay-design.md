# RAM Overlay Design

## Intent
The user's quick settings panel shows a green "1 >" chip (plus two small buttons) they don't want. They want a pill showing the current RAM percent drawn exactly over that spot, hiding the chip. Phone-only user; APK delivered through chat.

## Goals
- Opaque dark pill reading `RAM NN%` placed over the chip while the notification panel is open.
- NN = used / total physical memory, refreshed every 2 s.
- Pill position and size are user-set by drag/resize and persisted.

## Non-goals
- No root, no modifying the system UI, no network access, no analytics.
- No history, graphs, or notifications.

## Anti-goals
- Must not block touches or cover app content when the panel is closed.
- Must not show a stale or wrong number (0% or >100%).

## Constraints
- Overlay must use an accessibility service with `TYPE_ACCESSIBILITY_OVERLAY` (normal overlays sit under the panel).
- minSdk 26, compile/target 34, Kotlin, no third-party libraries.
- Built by GitHub Actions (`assembleDebug`); no local Android SDK.

## Approaches
1. Accessibility overlay (chosen): the only non-root way to draw over the panel. Costs a one-time accessibility grant.
2. Standard "draw over other apps" overlay: sits under the panel, so it cannot hide the chip. Rejected.
3. Custom system theme or hiding the chip via settings: depends on the ROM and could not be verified. Rejected.

## Decided by the user
- Show RAM percent in the overlay.
- Build as designed: accessibility overlay, panel-only visibility with an "always show" fallback, drag/resize calibration.

## Builder's calls
- Panel-open detection: an accessibility window from `com.android.systemui` covering at least 40% of the screen height.
- Calibration: drag the body to move, drag the bottom-right handle to resize, tap the check mark to save.
- Branch `ram-overlay` in the user's existing GitHub repo.
