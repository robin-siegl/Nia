# nia

A quiet Android launcher. Five everyday shortcuts, a clock and date, and a searchable app list made of names.

nia is a native Kotlin/Jetpack Compose MVP. It can become your actual default Home app. The first acceptance device is a Pixel 10 Pro using its normal personal profile.

<p>
  <img src="docs/screenshots/home-oled-grid.png" width="220" alt="nia OLED black icon grid" />
  <img src="docs/screenshots/home-violet-light.png" width="220" alt="nia pastel violet text list" />
  <img src="docs/screenshots/home-violet-dark.png" width="220" alt="nia dark violet text list" />
  <img src="docs/screenshots/color-picker.png" width="220" alt="nia color wheel with live light and dark previews" />
</p>

Rendered from the app's Compose screens; phone screenshots will also include the system bars.

## What works

- Fixed Phone, Messages, Music, Calendar and Maps shortcuts, each assigned to an installed app.
- Top-aligned icon grid or a text-only Home list.
- Apps completes the six-item icon grid or appears as a matching text entry. A settings gear is available on Home and in the app list.
- Small, Default or Large Home text, with a preview in Settings.
- Time opens Clock; date opens your assigned Calendar.
- Swipe up or tap **Apps** for an alphabetical, text-only app list.
- Search by name, with an optional automatic keyboard/focus setting.
- Optional alphabet navigation: drag the right edge of the app list to jump to a letter. Letters appear during interaction; searching hides the scrubber.
- First-launch setup and Android's default-launcher selection.
- Black/white or one-color themes, with **System / Light / Dark** modes.
- Use the visual color wheel and brightness slider, a preset, or a custom hex color: nia generates matching pastel/dark background and foreground pairs. Both variants preview live; wheel/slider changes save when released, and hex input uses Apply. Generated pairs target at least **7:1** text contrast.
- Pure `#000000` in monochrome dark mode, plus an OLED-black option for colored dark themes.
- Persistent preferences and versioned settings export/import, including app assignments.
- Missing-app recovery, live package-list updates and a settings reset/recovery path.
- Offline operation. The app requests no network, contacts, SMS, calendar-content or notification access.

## Install an MVP build

Open **[Actions → Checks](https://github.com/robin-siegl/Nia/actions/workflows/ci.yml)**. Choose a successful run and download the **nia-debug** artifact. Extract and install the APK, open nia, assign your apps and tap **Set as default launcher**. Android's Home-app chooser must be completed to use nia when pressing Home.

Development builds have their own package ID (`dev.robinsiegl.nia.debug`) so they can coexist with production builds. Prefer production APKs for long-term use and updates.

## Production APK in a few clicks

Configure the same four repository Actions secrets used by pip-os: `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS` and `ANDROID_KEY_PASSWORD`, using your nia keystore and credentials. The [one-time signing instructions](docs/RELEASING.md#one-time-signing-setup) include the keystore command and PowerShell copy-to-clipboard step.

If those secrets are already added, setup is complete. Open **[Actions → Production APK → Run workflow](https://github.com/robin-siegl/Nia/actions/workflows/production.yml)** on `main`. Leave the version blank for `0.2.0`, or enter a new version. Enable **publish_release** for a public APK download under **[Releases](https://github.com/robin-siegl/Nia/releases)**. The signed APK and checksum are also uploaded as the **nia-production-VERSION** artifact.

Keep a private backup of your keystore and passwords and use the same key for future updates. No additional signing-bundle secret is required. See [release instructions](docs/RELEASING.md) for the optional setup helper, local builds and troubleshooting.

## Build locally

Use JDK 17+, Android SDK Platform 36 and Build Tools 36.0.0. Install through Android Studio or `sdkmanager`, and set `ANDROID_HOME` or `sdk.dir` in `local.properties`.

```sh
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
```

On Windows use `gradlew.bat`. The wrapper includes a pinned Gradle distribution checksum. AGP 9.1.1 supplies built-in Kotlin; Compose and serialization plugins match Kotlin 2.2.10. Dependencies are pinned, rather than resolved through dynamic version ranges.

Minimum Android is 10 (API 29); the build compiles/targets API 36. The user's Android 17 phone remains the primary physical acceptance target. A lower target API does not prevent an app from running on a newer Android version.

## Verification

The test suite covers palette contrast across RGB samples, theme mode behavior, search normalization/order, backup validation/round trips, navigation, Home shortcuts, swipe access, search focus and screen rendering. Robolectric renders the Compose UI at API 35; screenshots are included in CI's test-report artifact. Host-side UI tests cannot establish Pixel gesture/Recents behavior: use the [device acceptance checklist](docs/DEVICE_TESTING.md) before calling the MVP phone-verified.

## Scope and next steps

The MVP supports the personal profile. Work profiles and Private Space are deferred and are not advertised as supported. Home categories remain fixed. Widgets, folders and feeds are outside this design.

- [Requirements and decisions](docs/REQUIREMENTS.md)
- [Release setup](docs/RELEASING.md)
- [Device acceptance](docs/DEVICE_TESTING.md)
- [Prioritized roadmap](docs/ROADMAP.md)
- [Changelog](CHANGELOG.md)

The source license is still to be selected.
