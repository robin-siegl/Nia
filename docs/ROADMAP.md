# Roadmap

Home content belongs to the user. Keep customization simple, preserve existing choices and keep the full app list quiet. These are priorities, not promised release dates.

## 1.5.0 in preparation

Version 1.4.0 is published. Prepare this Home interaction and command update as **1.5.0 / 1005000**. Publish only when the user requests it after validation.

| Area | Intended result |
| --- | --- |
| Home motion | One clock/date continuously shrinks while the shortcuts move upward in grid and list |
| Apps gesture | Header movement, then list scrolling, then a deliberate pull; no instruction text or progress ring, and one threshold haptic |
| Arrangement | Long-press drag on Home and in Settings → Home shortcuts, edge scrolling for long lists, and accessible alternatives |
| Home content | Zero to nine editable app/command shortcuts; no automatic, reserved or sticky Apps entry |
| App menu | Add to Home with a prefilled editor, App info and either Quick corner |
| Commands | Optional Open Apps, Search apps, Toggle flashlight, Start a timer, Set an alarm, Preset timer, Toggle theme and Sound controls on Home or in a Quick corner |

### Opening Apps from Home

1. Start with the expanded header. An upward drag consumes its height change and moves the shortcuts upward by the same amount. Draw the same clock/date throughout; do not crossfade a second header into place.
2. A long list then scrolls its configured entries. An optional Apps command is an ordinary entry and scrolls with everything else; it may be renamed, moved or removed.
3. Dragging beyond the end begins the resisted drawer preview. Show only a small chevron. Crossing the 112dp threshold gives one light haptic and arms opening; reversing below 72% disarms without another cue.
4. Release armed to open. Release early, reverse or cancel to close the preview. Momentum alone cannot arm or open Apps. A configured Open Apps command opens directly; Search apps additionally focuses search after settling.
5. Closing Apps preserves the Home position. Android Home and a new unlock restore the expanded header before the shortcut reveal. Keep unlock behavior separate from scroll-driven header movement.

The grid remains three columns and at most three rows, with no extra page or independent grid scroll. Header contraction moves it as a unit. The list scrolls every configured entry. Both layouts support an empty Home and expose Open Apps as an accessibility action, alongside swipe-up.

### Editing and commands

Keep one saved order across layouts. Preview a drag locally, save one validated order on drop and discard it on cancellation. Reject a stale order if entries were added or removed during the gesture. Keep keyboard/TalkBack alternatives and a small arrangement menu in Settings.

App-list Add to Home opens a draft with the app already assigned. Save applies the shortcut; Back discards it. Names and outline icons remain independent of the chosen app or command. The shared picker includes 193 bundled Material Symbols.

Commands are a typed allowlist, not arbitrary intents or scripts. Open Apps follows the normal keyboard preference; Search apps overrides it for that opening only. Toggle flashlight uses Android's public torch API and actual torch callbacks, handles unavailable hardware/camera resources and adds no camera permission or camera session. Start a timer hands duration selection and all timer behavior to a compatible Clock app through Android's timer action; nia adds no timer service. Set an alarm uses Clock's editor. Preset timers start a saved 1–1,440-minute duration in Clock, with shared 5/15/25-minute choices and a custom field. Toggle theme flips nia's visible Light/Dark choice and overrides System until Appearance selects it again. Sound controls opens Android's volume panel, falling back to sound settings if needed.

### Compatibility

Existing names, icons, app assignments, order, header targets and Quick corners survive the update. Removing the previously implicit Apps entry must not synthesize a new configured command. Fresh setup may leave Home empty; explain swipe-up during setup.

Schema **8** adds command targets and preset durations. Continue importing schemas **1–7**, including legacy Home and Settings-corner migrations, while rejecting command fields disguised as old schemas. Restore previews describe actual app/command changes and list only the configured order. Appearance reset preserves shortcut content; full reset returns to optional setup.

### Before publication

Use the manual [Dev APK action](RELEASING.md#trigger-a-dev-build) for signed branch builds that install as nia Dev with independent settings. Each successful run offers a 30-day artifact with checksum and source/build information, without publishing a release.

Run the automated checks and inspect native light/dark, intermediate-scroll and large-text renders. Then complete the relevant [Pixel checklist](DEVICE_TESTING.md), recording the actual OS build:

- Signed upgrade from 1.4.x, restart, old/current backup restores and an intentionally empty Home.
- Zero, one and nine entries in both layouts, both clock styles, maximum text/display scaling, commands and both corners.
- The reported eight-app-plus-optional-Apps-command list case: scroll all entries, continue into the drawer, reverse/cancel and repeat with haptics off or animations disabled.
- Real drags across grid rows, list and Settings edge scrolling, canceled drags, accessible arrangement and immediate taps/swipes afterward.
- Add to Home from filtered Apps, draft cancellation, full-Home feedback, command changes and search focus only after settling.
- Flashlight toggling, changes from Quick Settings, a busy camera, unavailable hardware and normal callback cleanup.
- Start a timer from Home and both corners, choose a duration in Clock, return to nia and verify that Clock rings and cancels normally. Check missing/disabled timer apps and retry without a blocked shortcut.
- Set an alarm in Clock, run independent preset timers, cancel duration edits, toggle nia's theme from System/Light/Dark and change volume through Android's panel. Check persistence, restore previews, large text and keyboard insets.
- Unlock, Battery Saver, Quick Settings interruption, Android Home and ordinary app returns without replaying unlock effects.

Keep physical-phone results separate from Robolectric simulations. The [design](DESIGN.md) and [architecture](ARCHITECTURE.md) guides own lasting product and implementation rules.

## Next: reliability and broader validation

Measure cold start, search latency, rendering and idle battery use on real hardware. Fix observed failures before adding more Home features. Test additional Android versions/manufacturers, system-controlled window resizing, foldables, external keyboards and accessibility tools as devices become available.

## Later possibilities

- **Theme-only sharing:** Export/import colors independently of app assignments; postponed.
- **Exact Pixel weather:** Investigate supported data access and correctly licensed matching icons. Generic substitutes and invented weather remain outside the plan.
- **Work profiles and Private Space:** Treat them as complete platform features, including identity, availability, locked visibility, search and restore rules.

## Project maintenance

Choose a source license, keep signing/release documentation current and consider meaningful emulator/device CI. Update checks stay manual. Keep the canonical guides aligned with implemented behavior.
