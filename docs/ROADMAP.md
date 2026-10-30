# After nia 1.0

Keep the five-category Home and text-only app list stable. Additions should improve daily use without introducing feeds, usage-ranked suggestions or an editable Home grid.

The 1.0 scope implements guided setup, focused Settings pages, smart search, About/privacy information and automatic public releases. Theme-only sharing remains postponed.

## 1. Phone validation and polish

Complete the Pixel 10 Pro checklist with the actual OS build. Fix gesture/keyboard edge cases, app launch failures, font scaling and any cutout/system-bar issues found there. Measure cold start, search latency and idle battery impact before setting optimization targets.

Done when the primary phone passes the checklist and an in-place signed update preserves preferences.

## 2. Theme refinement

The visual color wheel, brightness slider, curated presets and live paired previews are available in 0.2.0. Home text sizes and optional alphabet scrubbing are also implemented.

For a later release, allow importing/exporting just a theme independently from app assignments. Theme sharing is explicitly postponed. Consider adjustable pastel strength and respecting Android's enhanced contrast preference.

Done when any supported picker color produces readable matched light/dark pairs and System changes remain reliable.

## 3. Broader compatibility

Test additional Pixel/Android versions and one Samsung device. Improve landscape, foldable and tablet layouts as actual device needs emerge. Keep the app list accessible with external keyboards and switch-access tools.

Done when the documented device matrix has results rather than inferred compatibility.

## 4. Optional profile support

Implement work profiles with explicit profile identity, availability and paused-state handling. Separately implement Android's complete Private Space contract: a hideable container, lock/unlock, and exclusion of locked apps from search. Extend backup rules before allowing profile assignments.

Done when profile lock/unlock/removal and cross-device restore cannot leak or misbind app identities.

## 5. Distribution and maintainability

A changelog, privacy document and a production workflow using the committed Gradle version are available. Choose a source license, add a contribution guide, expand CI with emulator/device checks, and consider a public APK update-notification channel. Any network feature should remain optional and explain its purpose.

Done when releases have a stable signing identity, an upgrade test and clear compatibility notes.

## Deliberate scope boundaries

The two optional corner shortcuts are an explicit addition in 1.2.0. The five main categories retain their order. Arbitrary extra Home entries, rearrangement, widgets and social feeds remain outside the fixed minimal Home concept. Contacts, unread counts, embedded music playback and calendar-event previews would add data-access requirements and should each be justified independently.
