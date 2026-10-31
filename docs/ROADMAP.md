# Roadmap

Keep nia's fixed Home and text-only app list predictable. Future work should improve daily use without adding feeds, usage-ranked suggestions or an editable Home grid. These are priorities and possibilities, not promised release dates.

## First: reliability on the phone

Complete the [Pixel device checklist](DEVICE_TESTING.md) with the actual OS build. Focus on tap-to-wake versus power-button unlocks, gesture cancellation, keyboard behavior, Battery Saver, large text and in-place signed upgrades. Fix observed failures before adding more Home features.

Measure cold start, search latency, animation rendering and idle battery use on real hardware. Use those results to set performance targets; simulation alone cannot establish Pixel/SystemUI behavior or battery impact.

## Next: broader validation

Test more Android versions and manufacturers, then landscape, foldables and tablets as devices become available. Check external keyboards, TalkBack and other accessibility tools. Record actual results rather than inferring broad compatibility from one phone.

## Later possibilities

- **Theme-only sharing:** Export and import colors independently of app assignments. This remains postponed.
- **Exact Pixel weather:** Investigate only when supported data access and correctly licensed icons can meet the requested appearance. Generic substitute icons and invented weather are outside the plan.
- **Work profiles and Private Space:** Treat these as complete platform features with explicit identity, availability, locked visibility, search and restore rules. They are not currently supported.

## Project maintenance

Choose a source license, keep build/signing documentation current and consider emulator/device CI when it can cover meaningful gaps. Update checks remain manual. Keep the [design](DESIGN.md) and [architecture](ARCHITECTURE.md) guides aligned with implemented behavior.
