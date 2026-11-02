# Working on nia

nia aims to keep everyday tasks easy to reach while reducing visual clutter. Keep changes focused on that experience.

## Before changing the app

Read the [design guide](docs/DESIGN.md) and [architecture guide](docs/ARCHITECTURE.md). Coding agents must also follow [AGENTS.md](AGENTS.md) and its skill routing. Use the [roadmap](docs/ROADMAP.md) to distinguish current priorities from ideas for later.

A source license has not yet been selected. This guide describes the development workflow; it does not grant additional rights to the source. Preserve third-party copyright and license notices.

## Report a bug or suggest a feature

Search existing [issues](https://github.com/robin-siegl/Nia/issues) first, then choose **Bug report** or **Feature request** when opening an issue. For bugs, include your nia version or APK filename, build type, phone/Android version, steps and expected/actual behavior. Add layout, text size, Battery Saver or wake/unlock details when they seem relevant. Screenshots and logs are optional; remove personal information before posting.

For ideas, explain the task or frustration and the behavior you would like. Check the [roadmap](docs/ROADMAP.md) for current priorities. The issue chooser also links to installation and usage help. The forms become available after they are merged onto the default branch.

## Local setup

Install a full **JDK 17**, **Android SDK Platform 36** and **Build Tools 36.0.0**, through Android Studio or `sdkmanager`. Set `ANDROID_HOME`, or create an untracked `local.properties` containing `sdk.dir` for your SDK location.

Use the committed Gradle wrapper. Tool and dependency versions are declared in the Gradle files; avoid dynamic version ranges or unrelated upgrades.

```sh
./gradlew :app:assembleDebug
```

On Windows, use `gradlew.bat` in Command Prompt or `.\gradlew.bat` in PowerShell. The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

Debug builds use `dev.robinsiegl.nia.debug`, so they can coexist with production nia. Open the app and use Android's Home-app chooser to test it as a launcher.

For testing on your phone without a local build, use the manual [Dev APK action](docs/RELEASING.md#trigger-a-dev-build). It creates an optimized, signed **nia Dev** installation with its own settings and a repeatable signing identity. CI's ordinary debug APK uses its runner's debug key and is a separate channel.

## Check the change

Run checks that exercise the affected behavior:

```sh
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
python3 -m unittest discover -s scripts/tests -v
```

For release packaging or build changes, also run:

```sh
./gradlew :app:assembleDev :app:lintDev :app:assembleRelease :app:lintRelease
```

Unsigned local dev/release builds check packaging but cannot be installed directly. The dev variant inherits release optimization and uses `dev.robinsiegl.nia.dev`; without Actions metadata its version ends in `-dev`. See [Releasing](docs/RELEASING.md#local-signed-release) for signing.

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

## Changing branding

Use the [brand kit](branding/README.md) for the canonical mark, wordmark, colors, exports and usage rules. Edit its two SVG masters, then regenerate the brand assets and Android resources together:

```sh
python3 -m pip install -r scripts/branding-requirements.txt
python3 scripts/sync_branding.py
python3 scripts/sync_branding.py --check
```

Regeneration also needs a local Cairo runtime. Normal Android builds and the offline CI check do not need those export dependencies. Review icon masks, themed tints and native About/setup renders before committing the generated files. Follow the [physical icon checks](docs/DEVICE_TESTING.md#branding-and-app-icons) for Pixel behavior.

## Versions and changelog

Bump both `nia.versionName` and `nia.versionCode` in `gradle.properties` once for each completed change set, including documentation and skill changes. Choose the version according to the [version policy](docs/RELEASING.md#version-policy), and add a concise entry to [CHANGELOG.md](CHANGELOG.md).

Validate the committed values against the locally fetched release tags:

```sh
git fetch origin --tags
python3 scripts/release_version.py
```

This validates the version; it does not bump it. GitHub's production workflow builds exactly the committed values.

## Prepare a review

Work on a branch and open a pull request against `main`. Keep it up to date with `main`, let all required checks pass and resolve review conversations before merging. A second person's approval is optional so the solo maintainer can merge their own changes.

Explain the problem, resulting behavior and checks performed. Include a native screenshot for visible changes and note any remaining device-specific validation. Keep unrelated formatting and dependency updates separate.

Update the guide that owns a changed rule or boundary, then link to it from other docs. Add current decisions to the design or architecture guide; use the changelog for release history. Keep upstream license files intact.

Never commit keystores, passwords, local SDK paths or build outputs. Publishing an APK is a separate, manually triggered step described in [Releasing](docs/RELEASING.md).

## Main branch protection

[main.json](.github/rulesets/main.json) defines the **Protect main** branch ruleset:

| Rule | Requirement |
| --- | --- |
| Target | Exactly `main`; active enforcement |
| Changes | Pull requests with all review conversations resolved; zero required approving reviews |
| Checks | Unit and UI tests, Android lint, Debug APK and Dev APK validation, from GitHub Actions |
| Freshness | Branch must be up to date with `main` before merging |
| History | Force pushes and branch deletion blocked |
| Bypasses | None; maintainers and admins use the same PR workflow |

The JSON is a reusable configuration, not automatic enforcement. A repository admin must create the live rule in GitHub's repository **Settings → Rulesets**, importing this file or applying the same options. Check for an existing **Protect main** rule before creating one; update it rather than adding a duplicate.

Alternatively, an admin with an already-authorized GitHub CLI session can create the rule from a checkout:

```sh
gh api --method POST repos/robin-siegl/Nia/rulesets --input .github/rulesets/main.json
```

Verify that the live rule is **Active**, targets `main` and lists all four required checks. If a CI job name changes, update both this configuration and the live rule so merging does not wait for a check that no longer exists. The standard Checks workflow runs on every pull request, including documentation changes. Leave Production APK and Dev APK publication workflows outside the required-check list because they are manually triggered.
