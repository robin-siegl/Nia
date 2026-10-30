# Pixel lock-screen alignment

The portrait header is calibrated against the supplied 688 × 1536 Pixel 10 Pro lock-screen and Home screenshots. The original screenshots are not distributed in this repository.

The [rendered Home capture](screenshots/home-pixel-reference.png) shows the resulting layout without system bars.

| Visible landmark | Left | Top | Width | Height |
| --- | ---: | ---: | ---: | ---: |
| Clock sample `14 03` | 46 | 186 | 272 | 89 |
| Date sample `Fri, 30 Oct` | 54 | 350 | 126 | 23 |

These are measured text-ink bounds in reference pixels, not Android view bounds. Anchors scale with window width. The Home surface subtracts its already-applied safe-drawing top inset once, so the clock/date occupy the intended full-screen positions. App shortcuts begin below the date's touch target, and settings stays accessible at the bottom.

The header uses locally bundled Google Sans-derived Regular and SemiBold fonts with calibrated ink sizes and widths. OLED monochrome uses the reference's soft clock and white date; other themes retain their shared foreground color. Time and date retain their Clock and Calendar tap actions, spoken labels and at least 48dp touch targets.

## Appearance controls

Settings → Home → Pixel clock layout selects the aligned portrait header. Its clock/date metrics stay independent of Home app text sizes. Turning it off restores the previous scalable clock/date behavior. Landscape uses the compact layout. The setup and Settings previews reflect this choice.

This matches the supplied clock style and settings. nia does not read or automatically track Pixel's lock-screen clock selection, width slider, animation state or future OS layout changes. A different system clock style may need a different calibration.

## Weather status

The reference contains two separate weather presentations: the large outline complication attached to the system clock, and the small colored weather/temperature beside the date. Neither is synthesized in nia.

Android's [Smartspace manager](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/core/java/android/app/smartspace/SmartspaceManager.java) is a system API. The platform [permission definitions](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/core/res/AndroidManifest.xml) restrict its management/access, and the [clock weather data flow](https://android.googlesource.com/platform/frameworks/base/+/4c0f7a37669d1dc982045a6b07ac92127cfb5761%5E!/) is inside SystemUI. These are not a supported public component ordinary launchers can embed.

Google's public Weather API publishes weather-condition images, but it does not supply the Pixel lock-screen clock component. Exact Pixel weather integration remains deferred until a supported data and icon source can meet the requested appearance. No Internet/location permission, placeholder condition or generic weather symbol has been added.
