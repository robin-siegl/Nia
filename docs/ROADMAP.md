# Roadmap

Keep nia's Home and text-only app list predictable. Home content belongs to the user: customization should be simple, while upgrades preserve existing choices. These are priorities and possibilities, not promised release dates.

## 1.4.0 in preparation

Prepare **1.4.0 / 1004000** in the dedicated draft branch. Do not publish until the selected enhancements are implemented, checked and reviewed. Keep this version throughout the ongoing change set.

| Issue | Type | Status |
| --- | --- | --- |
| [#9: Remove landscape mode](https://github.com/robin-siegl/Nia/issues/9) | Bug | Portrait activity request staged; physical phone verification pending |
| [#10: Rearrange grid icons](https://github.com/robin-siegl/Nia/issues/10) | Enhancement | Planning; shared ordering and editor interaction proposed |
| [#11: Customize Home](https://github.com/robin-siegl/Nia/issues/11) | Enhancement | Requirements clarified: all app shortcuts editable, eight apps plus Apps; implementation pending |

### Confirmed Home requirements

All main Home app shortcuts are editable. There are **no permanent Phone, Messages, Music, Calendar or Maps slots** in the 1.4.0 model. Users can add, remove, rename, reassign, change the outline icon and rearrange every app shortcut.

Allow **zero to eight app shortcuts plus the mandatory Apps entry**, making at most **nine main Home entries including Apps**. Apps stays visible and opens the full app list. Removing every app shortcut leaves a valid Apps-only Home, with the clock/date still available.

Name, app and icon are independent. For example: name **Google Maps**, launch the **Google Maps app**, and show the **map outline icon**. A new shortcut can start with the app's current name; changing the app later must not overwrite a name or icon the user chose. Custom Home names do not rename apps or become aliases in the alphabetical app list.

The working interpretation is that the nine-entry cap applies to the main Home area; the two existing optional bottom-corner slots remain separate. Count those corners too if the user selects a global cap during review.

### Arrangement editor

Recommend editing from **Settings → Home screen → Arrange shortcuts**. One editor can add an app, change its name/icon/target, remove it, and reorder the complete user-defined list. Keep Home's simple launch interactions and empty-space long press for Settings.

Recommend one saved order shared by grid and text list, with **Apps pinned last**. Use drag handles and accessible Move up/Move down actions in the editor. Persist names, icons, assignments and order through layout switches, restarts and upgrades. Keep the full app list alphabetical regardless of Home order.

Commit an added or edited shortcut as a validated unit, and keep an unfinished draft through app/icon selection and activity recreation. Canceling an edit leaves saved shortcuts unchanged. If an app becomes unavailable, keep its shortcut's name/icon/position and offer reassignment when tapped. Removing a shortcut does not uninstall its app.

### Fresh installation and onboarding

Adapt the existing onboarding rather than creating a second setup flow. Recommend an optional **Add your apps** stage with **Add app** and **Skip for now** actions. Users can add one or more shortcuts, customize them, and continue; skipping leaves **Apps only**, with no forced starter set. Layout, appearance and default-launcher setup remain available, as do settings restore and access to the full app list.

Start a fresh installation with no main Home app shortcuts. Do not automatically add the old five categories. An optional starter suggestion could be considered later, but must never be required to continue.

Keep completed users out of onboarding during upgrades. Migrate their existing Home and preserve its assignments; the new empty-install default must not wipe their setup or replace it with a fresh selection flow.

### Grid and list behavior

- **Grid:** Three columns, up to three rows, with no vertical scrolling or extra pages. Fit the available height by adapting row spacing while retaining 48dp touch targets, expanded-header anchors and corner access. Check the smallest supported phone/window configuration before promising nine entries everywhere; do not clip actions or silently hide saved entries.
- **Text list:** Scroll the Home shortcut area when large text or many entries need it. Progressively collapse the large clock/date into an accessible compact header during intentional scrolling. Keep a visible Apps row and corner controls reachable. Use the staged handoff below when pulling beyond the final entry.
- **Drawer:** Both grid and text list use the same resisted pull, drawer preview, progress cue, haptic thresholds and release/cancel behavior. A grid or fitting/empty list enters this gesture directly. An overflowing list first scrolls its entries, then passes further finger drag into the same gesture at its end. Tapping Apps opens the shared transition directly.
- **Motion:** Derive reveal row counts from the configured entries, including the Apps-only case. Keep the expanded header stationary during unlock reveals; header collapse is driven by intentional Home-list scrolling. Preserve Reduced/system-disabled animations and input recovery after cancellation.
- **Header actions:** Keep Clock and Calendar targets independent of the Home shortcut list. Deleting or renaming a Calendar shortcut must not break the date action. Preserve legacy Calendar/Clock choices during migration; allow configuring header apps in Settings.

### Opening Apps from Home

The Apps pull gesture, haptic feedback, progress cue and clock transition apply to **both grid and text-list modes**. The grid itself does not scroll. An overflowing text list consumes normal Home scrolling first, then hands off to the same drawer gesture. Keep the chosen app text size instead of shrinking names to force a large list onto one screen.

1. **At rest:** Show the expanded clock/date at the existing Pixel-style position. Keep the visible Apps action and corner controls available. All grid entries fit; a long text list may overflow.
2. **Start the interaction:** In grid mode or a list that fits, an upward drag enters the Apps pull gesture directly. In an overflowing list, first collapse the clock/date into a readable compact header and scroll the Home entries; further direct dragging at the end enters the Apps pull gesture.
3. **Shared pull phase:** Reveal Apps with resistance under the finger in either view. Couple the clock's compact transition to this gesture, retaining any collapse already caused by list scrolling. Show the same quiet chevron/progress feedback. Grid mode keeps its normal Apps icon, and text mode its Apps row; any extra hint appears only during the pull.
4. **Arm opening:** Give a soft haptic when entering the Apps pull phase and a firmer cue when crossing the ready threshold. Show **Release to open Apps** when armed. Use distinct threshold/cancel bounds so small movements do not repeatedly toggle the state or buzz.
5. **Release or reverse:** Release while armed to settle Apps fully open; release before the threshold or drag back to cancel. Smoothly reverse partial drawer/header motion to the prior Home presentation. Search autofocus waits until Apps has fully settled.

A normal list fling or momentum reaching the end must not arm or open Apps. Use the same distance-based arming contract in both views; velocity alone does not replace crossing the threshold. A direct tap on Apps uses the same opening animation without requiring a pull.

This borrows the familiar pull-to-refresh interaction, with an upward pull opening another view. It is not a refresh operation and should not show a loading spinner or start network activity.

Use haptics for the two meaningful transitions, not for every row or pixel. Respect Android's haptic setting; visual cues and the Apps button must work when haptics are unavailable or disabled. Do not replay cues from recomposition or restored state. Reduced motion keeps the interaction and compact layout with simpler header changes; disabled animations must still settle immediately and release input.

Scrolling back toward the top restores the expanded clock. Canceling the pull or closing Apps returns to the Home presentation from before that pull: normally expanded in grid mode, potentially compact/scrolled in list mode. An explicit Android Home request or a new unlock resets to the expanded header and start of the list; perform the unlock reset while Home is covered/locked, before the visible shortcut reveal, to avoid a clock jump.

Threshold distance, resistance and compact-header proportions need a native prototype and Pixel testing in both views. The handoff must remain deliberate with the largest supported text/display sizes, and must never require haptics to be understood.

### Implementation outline

1. Replace fixed-category rendering with one ordered list of user-defined Home shortcuts. Give each shortcut a stable ID, display name, outline-icon choice and app target. Permit an unassigned target for migrated/unavailable entries; keep Apps as a generated, reserved destination.
2. Centralize entry resolution so Home, previews, onboarding and the arrangement editor use the same content. Do not keep a separate list of immutable built-ins or treat the first five entries as special. Identity belongs to the shortcut ID, not its name or target app.
3. Validate zero to eight entries, unique IDs, labels, icons and launch targets. Keep edits transactional and do not serialize arbitrary intents. Keep Clock, Calendar and corner targets separate.
4. Plan backup schema **6**, retaining imports for schemas 1–5. Convert each legacy category to an ordinary editable shortcut, preserving its label, category icon, target and order, including unassigned entries. Seed the independent date target from the old Calendar assignment.
5. Apply equivalent migration to existing on-device preferences. Distinguish an absent legacy shortcut-list field from an explicitly saved empty list: Apps-only must remain empty after restart, restore or appearance reset. Keep legacy corner migrations working.
6. Include names, icons, targets, order and header targets in restore previews and revalidation. Preserve Home content and corner configurations through normal updates and appearance reset. Full reset starts the new optional onboarding.
7. Replace the fixed-category assignment UI with the Home editor and separate header-app controls. Update the shared previews, setup, native tests and current product documentation when implementation is complete.

### Acceptance checks

- A fresh installation can finish onboarding after adding shortcuts or skipping. Skip produces Apps-only Home; restart and appearance reset do not seed the old five entries.
- An upgrade preserves the existing five-entry layout as ordinary editable shortcuts, with names/icons/targets intact. Existing completed users do not repeat onboarding.
- Add, edit, remove and reorder any shortcut, including migrated Phone/Maps/Calendar entries. Verify the Google Maps name/app/map-icon example and retain custom names/icons on reassignment.
- Test zero, one and eight app shortcuts. Reject a ninth app shortcut in the editor and imported data, since Apps already occupies one of the nine main entries.
- Reorder by touch and accessible actions; preserve identity and content through layout switches, restart, restore and signed upgrade. Restore an explicitly empty list without inventing entries.
- Verify a three-by-three grid on the Pixel with display/font scaling, corners and either clock style. The grid must not scroll. Apps always stays reachable, and the full app list stays alphabetical.
- Remove the Home Calendar shortcut and verify the independently assigned date action. Missing app targets retain recoverable Home entries.
- Test the common Apps pull in both grid and list: progressive header/drawer motion, progress cue, both haptic thresholds, reversal and release. Repeat with haptics disabled and with Reduced/zero-duration animations.
- Test an eight-shortcut Home plus Apps at large text: list scrolling must hand off only after its end; the grid must never scroll. A fling alone must not arm or open Apps.
- Test interruption by screen lock, Quick Settings, canceled pointers and Android Home. Dismissing Apps preserves the Home position; a new unlock restores the expanded header before revealing shortcuts. Preserve immediate real-tap/swipe input without a clock tick.
- Verify the visible Apps button, keyboard/TalkBack access, Apps-only Home and a list that fits without scrolling. Open search only after settling, and keep drawer scrolling independent from Home scrolling.
- Review native previews and complete physical portrait/unlock/gesture checks before marking the release ready.

### Remaining interaction choices

Settings-only reordering, one order shared across layouts, Apps pinned last and an always-reachable Apps row in text mode remain the recommended interaction defaults. Both views share the Apps pull/haptic experience above; only an overflowing list needs a Home-scrolling phase before it. The current cap interpretation leaves bottom corners separate from the main nine entries. These choices can be refined during review.

The fully editable shortcut model and the nine-entry main-area limit replace the earlier five-built-ins-plus-three-extras proposal. The customization and revised onboarding are not implemented yet.

## First: reliability on the phone

Complete the [Pixel device checklist](DEVICE_TESTING.md) with the actual OS build. Focus on tap-to-wake versus power-button unlocks, gesture cancellation, keyboard behavior, Battery Saver, large text and in-place signed upgrades. Fix observed failures before adding more Home features.

Measure cold start, search latency, animation rendering and idle battery use on real hardware. Use those results to set performance targets; simulation alone cannot establish Pixel/SystemUI behavior or battery impact.

## Next: broader validation

Test more Android versions and manufacturers, then system-controlled window resizing, foldables and tablets as devices become available. Check external keyboards, TalkBack and other accessibility tools. Record actual results rather than inferring broad compatibility from one phone.

## Later possibilities

- **Theme-only sharing:** Export and import colors independently of app assignments. This remains postponed.
- **Exact Pixel weather:** Investigate only when supported data access and correctly licensed icons can meet the requested appearance. Generic substitute icons and invented weather are outside the plan.
- **Work profiles and Private Space:** Treat these as complete platform features with explicit identity, availability, locked visibility, search and restore rules. They are not currently supported.

## Project maintenance

Choose a source license, keep build/signing documentation current and consider emulator/device CI when it can cover meaningful gaps. Update checks remain manual. Keep the [design](DESIGN.md) and [architecture](ARCHITECTURE.md) guides aligned with implemented behavior.
