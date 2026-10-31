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

- **Grid:** Three columns, up to three rows, with no vertical scrolling or extra pages. Fit the available height by adapting row spacing while retaining 48dp touch targets, header positions and corner access. Check the smallest supported phone/window configuration before promising nine entries everywhere; do not clip actions or silently hide saved entries.
- **Text list:** Allow the Home shortcut area to scroll when large text or many entries need it. Recommend a fixed, clearly separated Apps row styled like the other text entries, so it stays reachable. Keep clock/date and corner controls outside that scrolling area.
- **Drawer:** Reuse the current finger-following transition for Apps tap and swipe. Define the boundary between list scrolling and opening Apps explicitly: scrolling entries must not open the drawer accidentally. Validate deliberate swipe behavior with large text and a list at its end before choosing the final handoff.
- **Motion:** Derive reveal row counts from the configured entries, including the Apps-only case. Preserve the stationary header, Reduced/system-disabled animations and input recovery after cancellation.
- **Header actions:** Keep Clock and Calendar targets independent of the Home shortcut list. Deleting or renaming a Calendar shortcut must not break the date action. Preserve legacy Calendar/Clock choices during migration; allow configuring header apps in Settings.

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
- Test large-text Home lists separately from drawer scrolling, autofocus, keyboard dismissal, Back and Android Home. Preserve immediate real-tap/swipe input after normal, canceled and disabled animations.
- Review native previews and complete physical portrait/unlock/gesture checks before marking the release ready.

### Remaining interaction choices

Settings-only reordering, one order shared across layouts, Apps pinned last and a fixed Apps row in text mode remain the recommended interaction defaults. The current cap interpretation leaves bottom corners separate from the main nine entries. These choices can be refined during review.

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
