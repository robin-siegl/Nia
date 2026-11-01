# Working on nia

nia aims to keep everyday tasks easy to reach while reducing visual clutter. Keep changes focused on that experience.

## Before changing the app

Read the [design guide](docs/DESIGN.md) and [architecture guide](docs/ARCHITECTURE.md). Coding agents must also follow [AGENTS.md](AGENTS.md) and its skill routing. Use the [roadmap](docs/ROADMAP.md) to distinguish current priorities from ideas for later.

A source license has not yet been selected. This guide describes the development workflow; it does not grant additional rights to the source. Preserve third-party copyright and license notices.

## Local setup

Install a full **JDK 17**, **Android SDK Platform 36** and **Build Tools 36.0.0**, through Android Studio or `sdkmanager`. Set `ANDROID_HOME`, or create an untracked `local.properties` containing `sdk.dir` for your SDK location.

Use the committed Gradle wrapper. Tool and dependency versions are declared in the Gradle files; avoid dynamic version ranges or unrelated upgrades.

```sh
./gradlew :app:assembleDebug
```

On Windows, use `gradlew.bat` in Command Prompt or `.\gradlew.bat` in PowerShell. The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

Debug builds use `dev.robinsiegl.nia.debug`, so they can coexist with production nia. Open the app and use Android's Home-app chooser to test it as a launcher.

## Check the change

Run checks that exercise the affected behavior:

```sh
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
python3 -m unittest discover -s scripts/tests -v
```

For release packaging or build changes, also run:

```sh
./gradlew :app:assembleRelease :app:lintRelease
```

An unsigned local release build checks packaging but cannot be installed directly. See [Releasing](docs/RELEASING.md#local-signed-release) for signing.

Use focused tests while iterating. Before finishing app changes, run the relevant wider checks and review rendered screens when the UI changes. Prefer tests of observable behavior over tests that duplicate implementation details.

Documentation-only changes need link, example and skill-format checks, plus version validation. They do not need a new UI test just to assert wording. The existing GitHub **Checks** workflow still runs its normal tests, lint and debug build.

### Physical-device checks

Robolectric tests render the native Compose UI at API 35. They cover state and interactions but do not reproduce Pixel SystemUI, the real keyboard or every Battery Saver policy.

Use [Device testing](docs/DEVICE_TESTING.md) for launcher selection, gestures, unlocks, accessibility and signed upgrades. Record the actual device and OS build, and distinguish observed results from simulations. Screenshots are written to `app/build/reports/screenshots/` and included in CI's test-report artifact.

## Changing shortcut icons

Use the shared picker and catalog for Home shortcuts and Quick corners. Edit [material_symbols.json](scripts/material_symbols.json), keeping existing IDs stable, then run:

```sh
python3 scripts/sync_material_symbols.py --download
python3 scripts/sync_material_symbols.py --check
```

The import uses the pinned Google source revision and generates the Kotlin catalog/resource mapping and bundled XML vectors. Commit those outputs together with the manifest and license. `--check` works offline and runs in CI; normal builds never download icons. Do not hand-edit generated files or reuse the legacy `SETTINGS` ID. See [third-party notices](THIRD_PARTY_NOTICES.md#shortcut-and-interface-icons).

## Versions and changelog

Bump both `nia.versionName` and `nia.versionCode` in `gradle.properties` once for each completed change set, including documentation and skill changes. Choose the version according to the [version policy](docs/RELEASING.md#version-policy), and add a concise entry to [CHANGELOG.md](CHANGELOG.md).

Validate the committed values against the locally fetched release tags:

```sh
git fetch origin --tags
python3 scripts/release_version.py
```

This validates the version; it does not bump it. GitHub's production workflow builds exactly the committed values.

## Prepare a review

Explain the problem, resulting behavior and checks performed. Include a native screenshot for visible changes and note any remaining device-specific validation. Keep unrelated formatting and dependency updates separate.

Update the guide that owns a changed rule or boundary, then link to it from other docs. Add current decisions to the design or architecture guide; use the changelog for release history. Keep upstream license files intact.

Never commit keystores, passwords, local SDK paths or build outputs. Publishing an APK is a separate, manually triggered step described in [Releasing](docs/RELEASING.md).
