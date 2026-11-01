# Design guide

nia gives everyday tasks a predictable place and keeps other apps available in a quiet list. Design for easy, deliberate access, with enough empty space for Home to feel calm. Avoid claims that the launcher can guarantee changes in attention or screen time.

## Contents

- [Home and navigation](#home-and-navigation)
- [Color and typography](#color-and-typography)
- [Pixel-style header](#pixel-style-header)
- [Settings and language](#settings-and-language)
- [Motion and input](#motion-and-input)
- [Accessibility and review](#accessibility-and-review)

## Home and navigation

Home is user-defined. Every shortcut can be added, removed, renamed, assigned an outline icon, pointed at an app or command and reordered. Keep **zero to nine shortcuts**. There is no automatic or reserved Apps entry; Open Apps and Search apps are optional commands.

| Area | Design rule |
| --- | --- |
| Icon grid | Three columns, up to three rows; the grid moves upward with the contracting header, with no extra pages |
| Text list | Scroll every configured entry beneath a morphing clock/date, then deliberately pull beyond the end to open the drawer |
| Shortcut content | Independent name, app target and outline icon, such as Google Maps with a map icon |
| Clock and date | Time opens Clock; date opens its independently assigned Calendar app |
| Quick corners | Optional Empty/App/Command slots, separate from the main-area limit |
| App list | Alphabetical app names and search; no app-brand icons, usage ranking or recommendations |
| Settings access | Gear in Apps and a touch-and-hold action on empty Home space |

Use **Settings → Home shortcuts** for editing, and long-press drag to arrange entries there or directly on Home. Grid/list share one order. The optional Apps command is an ordinary movable entry. App-list Add to Home opens a prefilled editor. The fresh-install **Your shortcuts** step can add apps or commands or leave Home empty. Upgrades retain existing choices and do not repeat completed setup.

Legacy categories migrate into ordinary editable shortcuts. See [1.5.0 validation](ROADMAP.md#150-in-preparation) for the remaining device checks.

Keep the rest of Home open. Folders, widgets, badges and feeds remain outside this design. Settings cannot be assigned to a corner. Custom Home labels do not rename apps in the full app list.

Swipe up and an optional Open Apps command open the same drawer. Search apps opens it with search focused after settling. Expose Open Apps through Home accessibility actions when no visible command is configured. Pulling down dismisses it only when its list is already at the top. Back dismisses the keyboard before closing Apps; Android Home returns to a clean Home without an old query or keyboard. App selectors keep selection simple; app action menus belong in the full app list.

## Color and typography

Use the shared `Palette` and `NiaTheme` across launcher-owned surfaces.

| Theme | Background | Main foreground |
| --- | --- | --- |
| Monochrome dark | Exactly `#000000` | `#E6E6E6`, with the header tones below |
| Monochrome light | `#FFFFFF` | `#171717` |
| Color light | Generated pastel | Matching dark text and icons |
| Color dark | Generated dark tone, or pure black when selected | Matching light text and icons |

A single seed color generates both color themes. The palette targets at least 7:1 contrast for its main foreground/background pair. Check supporting text, outlines and disabled states separately. Preserve System, Light and Dark choices, and the optional pure-black background for colored dark themes.

Avoid surface tints, gradients or translucent wallpaper effects that turn the OLED canvas gray. System dialogs and the keyboard use Android's own styling.

Use Material typography for controls: readable body text, quieter supporting text and a clear page title. Reuse the existing 8/12/16/24dp spacing rhythm and consistent outline icon weight. Settings use 24dp horizontal content padding and a maximum content width of 600dp. Keep Home's full-width header calibration separate from that settings constraint.

Shortcut glyphs use the bundled **Material Symbols Outlined**, with unfilled paths, weight 300, grade 0 and 24px optical size. Tint them with the shared foreground. Keep one family across Home, Quick corners and the picker. Use the stable catalog IDs rather than labels or positions; see [icon attribution](../THIRD_PARTY_NOTICES.md#shortcut-and-interface-icons).

The icon picker is a reusable full-screen dialog. Search covers names, category words and synonyms across the entire catalog; selecting a category starts a browse view. Keep search, Close and confirmation available while results scroll. Use a category menu and larger cells at large text sizes, and simplify controls when the keyboard reduces the available height.

Respect system text scaling in controls and app names. The calibrated Pixel-style header keeps fixed proportions at rest; offer the existing scalable clock layout when that treatment is unsuitable.

## Pixel-style header

The default portrait header follows a supplied **688 × 1536 Pixel 10 Pro reference**. These are text-ink measurements in reference pixels, not view bounds:

| Sample | Left | Top | Width | Height |
| --- | ---: | ---: | ---: | ---: |
| Clock `14 03` | 46 | 186 | 272 | 89 |
| Date `Fri, 30 Oct` | 54 | 350 | 126 | 23 |

Scale anchors by window width. Subtract the already-applied safe-drawing top inset once. Keep the shortcuts below the date's touch target and retain accessible Clock/Calendar actions. See [PixelHeaderLayout.kt](../app/src/main/java/dev/robinsiegl/nia/model/PixelHeaderLayout.kt), [PixelClockHeader.kt](../app/src/main/java/dev/robinsiegl/nia/ui/PixelClockHeader.kt) and the [reference render](screenshots/home-pixel-reference.png).

Use the bundled Nia Header font subsets. In monochrome OLED mode, the clock uses `#E2E2E2` and the date white; colored themes share their generated foreground. Preserve the attribution in [third-party notices](../THIRD_PARTY_NOTICES.md).

**Settings → Home screen → Pixel-style clock** controls this layout. Turning it off restores the scalable clock. nia requests portrait orientation on phones; the compact layout remains a fallback for windows whose shape Android controls. nia does not read or track the system clock style, size slider or future OS changes. The original phone screenshots are not distributed here.

Exact Pixel weather remains deferred. The reference's large outline clock complication and small colored condition icon are separate elements. Do not replace them with generic symbols, invented conditions or assets of uncertain provenance. See the [platform boundary](ARCHITECTURE.md#platform-boundaries).

## Settings and language

Keep the overview short and show useful current choices. Group related controls on focused pages. Keep Back and the page title visible while the content scrolls.

- Use sentence case and direct labels such as **Keyboard on open**, **A–Z navigation**, **Save backup** and **Restore backup**.
- Use lowercase **nia**. Match **Home**, **Apps** and other destination names to their visible labels.
- Explain the outcome or current value in supporting text. Avoid jargon, repeated labels and long implementation explanations.
- Make a switch's entire row tappable, with one accessible switch action. Expose mutually exclusive choices as a single-selection group.
- Show the chosen app, command or icon and make changing it discoverable. Use the shared icon picker for Home shortcuts and Quick corners. Show labeled tiles, search and categories, preview a pending selection, and apply only after confirmation. Cancel leaves the saved choice unchanged.
- Group custom color controls together, with live light/dark previews. Save wheel/slider changes on release and valid hex input through Apply.
- Explain what each reset clears and preserves. Confirm both reset actions. Keep the restore preview's labels consistent with Settings.

Keep product copy calm and useful. Avoid blame, urgency, engagement prompts and promises about behavior the app cannot verify. Error messages should explain the next available action without hiding Home or Apps.

## Motion and input

Animate transitions to explain where content moves. Keep Home mounted beneath the Apps drawer and keep the clock/date stationary during shortcut reveals.

| Transition | Current treatment |
| --- | --- |
| Apps drawer | Follows the finger, then settles with a shared tap/Back transition; Standard settling is distance-dependent, up to 280ms |
| Unlock shortcuts | 120ms lead-in, 360ms row fades, 24ms row stagger and up to 16dp upward movement |
| Reduced | Short 120ms transitions; shortcut reveal uses a fade without the lead-in, movement or stagger |
| Android animations disabled | Reach the settled state without animated movement, and restore input |

All timing inherits Android's animation-duration scale. The unlock lead-in is a visual heuristic, not a signal that SystemUI has finished. nia controls its own shortcuts; Android controls the lock-screen exit.

Delay search autofocus until the drawer settles. Block accidental launches during transitions, then release input after completion or cancellation. Press feedback stays subtle and uses Android's haptic preference. Keep lifecycle mechanics in the [architecture guide](ARCHITECTURE.md#unlock-and-input-reliability).

### Home scrolling and the Apps pull

Keep text at the selected size. Transform the same clock/date continuously as the header contracts; do not fade between expanded and compact copies. Its changing height moves the shortcuts upward in both layouts. The header remains expanded and stationary for unlock reveals.

Consume header movement first, then an overflowing list's scroll, then the resisted Apps pull. The list includes every configured entry, including an optional Apps command. A grid moves as a unit with the header. Show a small chevron during the pull, with no instruction text or progress ring. Give one light haptic when opening is armed; do not buzz at the start or again on reversal. Release armed to open and reverse or release early to cancel. A fling alone cannot arm or open Apps.

Keep direct user movement responsive to the finger and respect Reduced/disabled animations when settling. Closing Apps preserves the Home position; Android Home and fresh unlocks restore the expanded header. See the [gesture sequence](ROADMAP.md#opening-apps-from-home).

### Arranging shortcuts

A long press lifts an entry for dragging. Move surrounding entries to make the proposed position clear, keep the dragged item under the finger, and scroll long lists near their edges. Save once on drop; cancel restores the saved arrangement. Use the same interaction on Home and in Settings. Keep accessible Move up/down actions and the Settings arrangement menu.

App action menus use a clear app heading, grouped actions and matching outline glyphs. Add to Home opens a draft with the target assigned; make the nine-shortcut limit clear without blocking App info or corner assignment. App selection screens do not offer this menu.

## Accessibility and review

Provide at least 48dp touch targets, meaningful spoken labels for icon-only actions, visible focus/selection states and a visible alternative to gestures. Keep a non-gesture Open Apps accessibility action and controls reachable with large text, display scaling, keyboard insets, system-controlled window resizing and screen cutouts. A short grid window can use the compact header to retain all nine minimum-size touch targets. Hidden Home content must not remain a duplicate accessibility destination beneath Apps.

Review real Compose renders in monochrome and color light/dark themes, plus a narrow screen with large text. Use the [device checklist](DEVICE_TESTING.md) for TalkBack, keyboard and system gestures. Do not present a mockup or native test render as a physical-phone result.

Reference captures: [nine-entry grid](screenshots/home-nine-grid.png), [large-text list](screenshots/home-nine-list-large.png), [header mid-scroll](screenshots/home-scroll-mid.png), [drag preview](screenshots/home-drag-preview.png), [Home shortcut editor](screenshots/home-shortcut-editor.png), [command picker](screenshots/shortcut-command-picker.png), [Settings](screenshots/settings-overview.png), [app actions](screenshots/app-actions.png), [icon picker](screenshots/shortcut-icons.png), [light picker](screenshots/icon-picker-light.png), [large-text picker](screenshots/icon-picker-large-text.png) and [restore preview](screenshots/restore-preview.png).

### Android guidance

Use [settings guidance](https://developer.android.com/design/ui/mobile/guides/patterns/settings), [Compose accessibility defaults](https://developer.android.com/develop/ui/compose/accessibility/api-defaults) and [inset handling](https://developer.android.com/develop/ui/compose/system/insets-ui) and [event-based haptics](https://developer.android.com/develop/ui/views/haptics/haptic-feedback) as platform references. Adapt them to nia's minimal Home and user-defined shortcuts rather than adding controls simply because a component exists.
