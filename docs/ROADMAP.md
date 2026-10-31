# Roadmap

Keep nia's Home and text-only app list predictable. The proposed customization below should stay optional and preserve the existing default experience. These are priorities and possibilities, not promised release dates.

## 1.4.0 in preparation

Prepare **1.4.0 / 1004000** in the dedicated draft branch. Do not publish until the selected enhancements are implemented, checked and reviewed. Keep this version throughout the ongoing change set.

| Issue | Type | Status |
| --- | --- | --- |
| [#9: Remove landscape mode](https://github.com/robin-siegl/Nia/issues/9) | Bug | Portrait activity request staged; physical phone verification pending |
| [#10: Rearrange grid icons](https://github.com/robin-siegl/Nia/issues/10) | Enhancement | Planning; interaction and shared ordering need review |
| [#11: Customize Home](https://github.com/robin-siegl/Nia/issues/11) | Enhancement | Planning; up to three optional extra shortcuts and non-scrolling icon grid |

### Proposed experience

Keep the default five everyday shortcuts and Apps exactly as today. Allow zero to three extra app shortcuts, making at most **nine grid entries including Apps**. Bottom-corner shortcuts remain separate from that grid limit. Existing app assignments, clock/date and colors carry over.

Recommend editing from **Settings → Home screen → Arrange shortcuts**. An editor can reorder the everyday and extra entries, add/remove extra shortcuts, and restore the default arrangement. Home itself keeps its simple launch interactions and empty-space long press for Settings.

Each extra shortcut gets an app, a matching outline icon and a short name, such as Browser or Notes. Keep the five existing category names and assignments. Extra entries can be removed; removing one also removes its order reference. If an app becomes unavailable, keep the entry's name/icon and offer reassignment when tapped.

Recommend one saved order shared by grid and text list, with **Apps pinned last**. Reorder in the editor using a drag handle, plus accessible Move up/Move down actions. Persist changes across layout switches, restarts and upgrades. Keep the full app list alphabetical regardless of Home order.

### Grid and list behavior

- **Grid:** Three columns, up to three rows, with no vertical scrolling or extra pages. Fit the available height by adapting row spacing while retaining 48dp touch targets, header positions and corner access. Check the smallest supported phone/window configuration before promising nine entries everywhere; do not clip actions or silently hide saved entries.
- **Text list:** Allow the Home shortcut area to scroll when large text or extra entries need it. Recommend a fixed, clearly separated Apps row styled like the other text entries, so it stays reachable. Keep clock/date and corner controls outside that scrolling area.
- **Drawer:** Reuse the current finger-following transition for Apps tap and swipe. Define the boundary between list scrolling and opening Apps explicitly: scrolling entries must not open the drawer accidentally. Validate deliberate swipe behavior with large text and a list at its end before choosing the final handoff.
- **Motion:** Derive reveal row counts from the configured entries. Preserve the stationary header, Reduced/system-disabled animations and input recovery after cancellation.

### Implementation outline

1. Add a shared Home-entry model: stable built-in IDs, stable extra-shortcut IDs, ordered references and at most three extras. Append the pinned Apps entry at presentation time.
2. Centralize entry resolution so Home, previews, onboarding and the arrangement editor show the same content and order. Keep Clock and corner assignments independent.
3. Extend preferences validation for entry IDs, order, labels and limits. Preserve the current five-category assignment format where possible; do not serialize arbitrary launch intents.
4. Plan backup schema **6**, accepting schemas 1–5 with the original default order and no extras. Include ordering/extras in restore previews, revalidate app availability and retain clear recovery for missing targets.
5. Preserve all app assignments, including extras and corner configurations, through normal updates and appearance reset. Keep arrangement reset explicit and full reset comprehensive.
6. Build the editor and adaptive grid/list changes only after the interaction decisions below are settled. Update the design/architecture guides and user copy with the implemented behavior.

### Acceptance checks

- An upgrade and every older supported backup retain the current default Home and existing assignments.
- Add zero, one, two or three extras; reject a fourth in the editor and in imported data. Apps stays reachable, and the full app list stays alphabetical.
- Reorder by touch and accessible actions; verify saved order, icon and app identities through layout switches, restart, restore and signed upgrade.
- Verify a three-by-three grid on the Pixel with display/font scaling, corners and either clock style. The grid must not scroll.
- Test large-text Home lists separately from drawer scrolling, autofocus, keyboard dismissal, Back and Android Home. Preserve immediate real-tap/swipe input after normal, canceled and disabled animations.
- Review native previews and complete the physical portrait/unlock/gesture checks before marking the release ready.

### Decisions to settle

1. Keep reordering in Settings, or allow dragging icons directly on Home?
2. Share the chosen order across grid and text list, or make reordering grid-only?

Apps pinned last, the existing five categories retained, and the three-extra limit are the proposed defaults. They can be revised during this planning phase. The enhancements are not implemented yet.

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
