# Roadmap

Home content belongs to the user. Keep customization simple, preserve existing choices and keep the full app list quiet. These are priorities, not promised release dates.

## 1.4.0 in preparation

The **1.4.0 / 1004000** change set includes portrait mode, editable Home apps and the shared Apps pull. Keep this version throughout the change set. Do not publish or trigger Production APK until validation is complete and the user requests publication.

| Issue | Result | Remaining validation |
| --- | --- | --- |
| [#9: Remove landscape mode](https://github.com/robin-siegl/Nia/issues/9) | Portrait requested throughout nia | Physical phone and return from landscape apps |
| [#10: Rearrange grid icons](https://github.com/robin-siegl/Nia/issues/10) | One editable order shared by grid/list | Signed upgrade and TalkBack on the Pixel |
| [#11: Customize Home](https://github.com/robin-siegl/Nia/issues/11) | Eight editable apps plus Apps; optional onboarding | Physical large-text and gesture checks |

### Home apps and arrangement

Use **Settings → Home apps** to add, remove, rename, reassign, choose an outline icon and reorder any app entry. Move up/down buttons support touch, keyboard and accessibility actions. Apps stays last and cannot be removed. Grid/list share one saved arrangement.

The main area supports **zero to eight app shortcuts plus Apps**. The two optional bottom corners are separate. Names, targets and icons are independent: Google Maps can have a Maps outline icon, and changing its target keeps the chosen name/icon. Custom Home labels do not rename alphabetical-list entries or become search aliases.

An unfinished editor draft stays through app/icon picking and activity recreation. Cancel leaves saved shortcuts alone. Saved entries survive restart; transient drafts/navigation do not survive process death. Unavailable apps retain editable names/icons/positions for reassignment.

### Setup, migration and backups

New installations start with no Home app entries. The optional **Add your apps** stage can add shortcuts or **Skip for now** to an Apps-only Home. Layout, colors, restore and default-launcher setup remain available.

Upgrades migrate the five legacy categories to ordinary editable entries, preserving names/icons/targets/order, including unassigned entries. Completed setup does not repeat. A saved empty list stays empty after restart, restore and appearance reset.

Clock and Calendar header targets are independent of Home entries. Migration preserves Clock and seeds Calendar from its old assignment. Configure them in **Settings → Home screen → Clock & date**.

Schema **6** exports names, icons, targets, order and header targets; schemas 1–5 remain importable. Restore previews show each actual change, sanitize unavailable targets while preserving Home entries, and revalidate before applying. Appearance reset keeps Home entries, header targets and corners; full reset starts optional onboarding again.

### Opening Apps from Home

Both views use the same pull, progress cue and haptic thresholds. The grid has three columns and at most three rows, with no vertical scrolling. Large text lists scroll their entries before handing off at the end, and pin Apps above the corner controls.

1. At rest, show the existing expanded clock/date. In an overflowing list, scrolling progressively reveals a compact header.
2. A direct upward drag in a grid/fitting list, or beyond an overflowing list's end, starts a resisted Apps preview. Show a quiet chevron/progress cue and one soft haptic.
3. Crossing the 112dp pull threshold arms opening, gives one firmer cue and shows **Release to open Apps**. Reversal below 72% disarms without repeated buzzing.
4. Release armed to settle open; release early, reverse or cancel to restore Home. A fling alone cannot arm/open Apps. Tapping Apps uses the shared transition directly.
5. Focus search only after settling. Closing Apps restores the previous Home scroll/header position. Explicit Android Home or a new unlock starts from the expanded header before the visible shortcut reveal.

Haptics follow Android's setting, require no extra permission and are never needed to understand the gesture. Reduced/disabled animations keep the interaction working and release input after cancellation. The system lock-screen exit remains controlled by Android.

### Before publication

Run the automated checks and inspect native light/dark and large-text renders. Then complete the relevant [Pixel device checklist](DEVICE_TESTING.md), recording the actual OS build:

- In-place signed upgrade from 1.3.x, optional fresh setup, restart, schema-1–6 restore and an intentionally empty Home.
- Zero, one and eight apps in both layouts, both clock styles, maximum font/display scaling, corners and TalkBack/keyboard access.
- Shared pull/release/cancel, short fast flings, list-to-drawer handoff, haptics off and Reduced/zero-duration animations.
- Immediate real taps/swipes after interruption, Battery Saver, Quick Settings and Android Home.
- Tap-to-wake versus power-button unlock with the same authentication method; expanded header before reveal and no replay on ordinary app returns.

The [design](DESIGN.md) and [architecture](ARCHITECTURE.md) guides own the lasting product and implementation rules. Keep physical-phone results separate from Robolectric simulations.

## Next: reliability and broader validation

Measure cold start, search latency, rendering and idle battery use on real hardware. Fix observed failures before adding more Home features. Test additional Android versions/manufacturers, system-controlled window resizing, foldables, external keyboards and accessibility tools as devices become available.

## Later possibilities

- **Theme-only sharing:** Export/import colors independently of app assignments; postponed.
- **Exact Pixel weather:** Investigate supported data access and correctly licensed matching icons. Generic substitutes and invented weather remain outside the plan.
- **Work profiles and Private Space:** Treat them as complete platform features, including identity, availability, locked visibility, search and restore rules.

## Project maintenance

Choose a source license, keep signing/release documentation current and consider meaningful emulator/device CI. Update checks stay manual. Keep the canonical guides aligned with implemented behavior.
