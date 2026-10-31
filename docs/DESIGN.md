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

Keep these entries in order: **Phone, Messages, Music, Calendar, Maps, Apps**.

| Area | Design rule |
| --- | --- |
| Icon grid | Three columns, two rows; simple outline category icons, with Apps as the sixth icon |
| Text list | The same entries as full-width text rows; Apps has the same treatment |
| Clock and date | Time opens Clock; date opens the assigned Calendar app |
| Bottom corners | Two fixed optional slots, each Empty or App, with an outline icon |
| App list | Alphabetical app names and search; no app-brand icons, usage ranking or recommendations |
| Settings access | Gear in Apps and a touch-and-hold action on empty Home space |

The five everyday shortcuts keep their names, symbols and order when their assigned apps change. Keep the rest of Home open. Extra Home pages, rearrangement, folders, widgets, badges and feeds are outside the current design. Settings cannot be assigned to a corner.

Swipe up and the visible Apps entry open the same drawer. Pulling down dismisses it only when its list is already at the top. Back dismisses the keyboard before closing Apps; Android Home returns to a clean Home without an old query or keyboard. App selectors keep selection simple; app action menus belong in the full app list.

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

Respect system text scaling in controls and app names. The calibrated Pixel-style header keeps fixed proportions; offer the existing scalable clock layout when that treatment is unsuitable.

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
- Show the chosen app or icon and make changing it discoverable. Highlight the selected corner icon.
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

## Accessibility and review

Provide at least 48dp touch targets, meaningful spoken labels for icon-only actions, visible focus/selection states and a visible alternative to gestures. Keep controls reachable with large text, display scaling, keyboard insets, system-controlled window resizing and screen cutouts. Hidden Home content must not remain a duplicate accessibility destination beneath Apps.

Review real Compose renders in monochrome and color light/dark themes, plus a narrow screen with large text. Use the [device checklist](DEVICE_TESTING.md) for TalkBack, keyboard and system gestures. Do not present a mockup or native test render as a physical-phone result.

Reference captures: [Settings](screenshots/settings-overview.png), [app actions](screenshots/app-actions.png), [corner icons](screenshots/shortcut-icons.png) and [restore preview](screenshots/restore-preview.png).

### Android guidance

Use [settings guidance](https://developer.android.com/design/ui/mobile/guides/patterns/settings), [Compose accessibility defaults](https://developer.android.com/develop/ui/compose/accessibility/api-defaults) and [inset handling](https://developer.android.com/develop/ui/compose/system/insets-ui) as platform references. Adapt them to nia's fixed, minimal Home rather than adding controls simply because a component exists.
