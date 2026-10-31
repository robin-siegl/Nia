# Working on nia

nia is a Kotlin/Jetpack Compose Android launcher. Follow these instructions for changes anywhere in this repository.

## Active release: 1.4.0

Keep the work for issues #9, #10 and #11 in the ongoing **1.4.0 / 1004000** change set. The bug fix and enhancement plans are staged in a draft PR. Keep this version while completing the agreed scope; do not publish or trigger Production APK until the work is finished and the user requests it. The fixed-Home rules below describe the implemented baseline; customization is still under design review. See the [1.4.0 plan](docs/ROADMAP.md#140-in-preparation).

## Read the relevant guidance

- Read [DESIGN.md](docs/DESIGN.md) for product, UI, copy, accessibility and motion changes.
- Read [ARCHITECTURE.md](docs/ARCHITECTURE.md) for code, state, data, lifecycle and platform changes.
- Follow [CONTRIBUTING.md](CONTRIBUTING.md) for setup and validation.

Use the repository skills below. If your agent does not discover skills automatically, open the linked `SKILL.md` and follow it directly.

| Work | Required skill |
| --- | --- |
| Kotlin, Compose, preferences, backups, dependencies, tests or Android build configuration | [nia-android-development](.agents/skills/nia-android-development/SKILL.md) |
| Home role, app discovery/launching, app lists, gestures, unlocks, lifecycle, keyboard, system navigation or profiles | [nia-launcher-development](.agents/skills/nia-launcher-development/SKILL.md), plus the Android skill |
| Documentation or skill changes | Read the guide being changed and apply the version policy below |

## Preserve the product

Keep Phone, Messages, Music, Calendar and Maps in their fixed order, with Apps available by gesture and a visible entry. Keep the app list alphabetical and text-only. Corner shortcuts support Empty or App; nia Settings is not a corner action.

Keep the personal-profile scope, offline core and manual-only update checks. Preserve exact OLED black, matched themes, accessible controls and Android's animation preferences. See the guides for the full behavior and known device limitations.

## Bump the version for every completed change set

Before handing off or merging any repository change, including docs and skills:

1. Read the current `nia.versionName` and `nia.versionCode` in `gradle.properties`, and check the latest version on `main` and stable release tags.
2. Bump once for the entire change set. Use **patch** for fixes, polish, copy, docs and maintenance; **minor** for compatible new features; **major** for intentional breaking changes. Reset lower components when increasing a higher one.
3. Update both properties together. Calculate `versionCode = major * 1000000 + minor * 1000 + patch`. Use a stable `MAJOR.MINOR.PATCH` without a `v` prefix or prerelease suffix.
4. Add a concise entry under that version in [CHANGELOG.md](CHANGELOG.md), and run `python3 scripts/release_version.py` after fetching release tags.

Do not bump again for each file, intermediate commit or fix within the same change set. Preserve a correct bump already made for that task. Follow an explicit user-specified version while retaining validation. Do not bump for a review that makes no changes.

Use the [release policy](docs/RELEASING.md#version-policy) for examples, limits and publication rules. Never move an existing release tag, rotate the production signing key, or substitute a workflow-generated version. Do not trigger the Production APK workflow unless requested.

## Validate and report

Run checks proportional to the change, using the commands in CONTRIBUTING.md. For UI changes, inspect real Compose renders with light/dark themes and large text. For lifecycle or gesture fixes, test cancellation and real pointer input without relying on a clock tick or unrelated recomposition.

Check Markdown links and skill metadata when changing docs or skills. Preserve attribution and bundled license files. Keep useful technical decisions in the canonical guides instead of adding another planning document.

Report what changed, which checks ran, the prepared version and any device validation still needed. Do not describe a simulated unlock test as a confirmed Pixel fix.
