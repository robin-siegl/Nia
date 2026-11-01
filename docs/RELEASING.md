# Releasing nia

Use **Dev APK** to test a branch through a downloadable build, or **Production APK** to prepare a public release. Both use the base version committed in `gradle.properties` and the same existing signing secrets.

If nia's four signing secrets are already configured, go straight to [Trigger a dev build](#trigger-a-dev-build) or [Trigger a production build](#trigger-a-production-build). No local Android build or manual Git tag is needed.

## Contents

- [One-time signing setup](#one-time-signing-setup)
- [Version policy](#version-policy)
- [Trigger a dev build](#trigger-a-dev-build)
- [Trigger a production build](#trigger-a-production-build)
- [Local signed release](#local-signed-release)
- [Before publishing](#before-publishing)
- [Troubleshooting](#troubleshooting)

## One-time signing setup

Use the same production key for every update. Keep a private backup of the keystore, alias and passwords outside this repository.

Generate a keystore with a full JDK:

```sh
keytool -genkeypair -v -keystore nia-release.jks -alias nia -keyalg RSA -keysize 3072 -validity 10000
```

If PowerShell cannot find `keytool`, Android Studio's bundled JDK is another option. Adjust this path if Studio is installed elsewhere:

```powershell
& 'C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe' -genkeypair -v -keystore nia-release.jks -alias nia -keyalg RSA -keysize 3072 -validity 10000
```

Add these **repository secrets** under **Settings → Secrets and variables → Actions → New repository secret**:

| Secret | Value |
| --- | --- |
| `ANDROID_KEYSTORE_BASE64` | The complete nia keystore encoded as one line of base64 |
| `ANDROID_KEYSTORE_PASSWORD` | Keystore password |
| `ANDROID_KEY_ALIAS` | `nia`, or the alias used when creating the key |
| `ANDROID_KEY_PASSWORD` | Private-key password; use the keystore password if they are the same |

On Windows, encode the keystore and copy the result to your clipboard:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes((Resolve-Path ./nia-release.jks))) | Set-Clipboard
```

On Linux use `base64 -w 0 nia-release.jks`; on macOS use `base64 -i nia-release.jks | tr -d '\n'`. Paste the result into `ANDROID_KEYSTORE_BASE64`. No additional signing-bundle secret is required.

### Optional helper for a new key

For a fresh setup, run `python scripts/setup_signing.py --upload` with an authenticated GitHub CLI. Windows users can run `.\scripts\setup-signing.ps1 --upload`. The helper finds `keytool`, creates a key, uploads the four secrets and keeps a local `.signing/` backup. Later runs reuse that backup.

Skip the helper if nia's key and secrets already exist. Back up `.signing/` privately. Without `--upload`, the helper creates the local backup and explains which fields belong in each secret without printing credentials.

## Version policy

**Agents must bump the project version once for every completed change set, including documentation and skill changes.** Update both properties in `gradle.properties` and add an entry to `CHANGELOG.md` before handing off or merging the work.

| Change | Bump | Example |
| --- | --- | --- |
| Fixes, UI polish, wording, docs, skills or maintenance | Patch | `1.3.2` → `1.3.3` |
| Compatible new functionality | Minor; reset patch | `1.3.3` → `1.4.0` |
| Intentional breaking change | Major; reset minor and patch | `1.4.0` → `2.0.0` |

Use the highest-impact change in the set. Do not bump again for individual files, intermediate commits or corrections within the same task. Preserve a correct bump already made for that task, and follow an explicit user-specified version when it passes validation. A review that changes nothing does not need a bump.

Read the current project version, the latest version on `main` and stable release tags before choosing the next version. The new version must advance the previous change set even if it was not published. Recheck before merging if another change set has advanced `main`.

Use stable `MAJOR.MINOR.PATCH`, without a `v` prefix or prerelease suffix. Calculate the Android code as:

```text
versionCode = major * 1000000 + minor * 1000 + patch
```

For example, the pair below describes version 1.4.0; it is an example, not a separate source of the current version:

```properties
nia.versionName=1.4.0
nia.versionCode=1004000
```

Keep major at most 1999 and minor/patch at most 999, with a positive code. If a component reaches its limit, advance the next higher component and reset the lower ones.

Fetch tags, then validate the prepared values:

```sh
git fetch origin --tags
python3 scripts/release_version.py
```

The validator checks the version/code pair and rejects versions at or below the latest stable `vMAJOR.MINOR.PATCH` tag. It does not edit files or bump anything. The production workflow also requires `main`. See [AGENTS.md](../AGENTS.md) for the agent workflow.

Dev builds validate the same committed version/code pair but allow any branch in this repository and versions that have already been released. Running or retrying a dev build never changes `gradle.properties` or advances the release version.

## Trigger a dev build

1. Push the changes you want to test to a branch in this repository.
2. Open **[Actions → Dev APK → Run workflow](https://github.com/robin-siegl/Nia/actions/workflows/dev.yml)** and select that branch.
3. Select **Run workflow**. Tests, lint and build validation run before the signed download is prepared.
4. After the run succeeds, use **Download the APK bundle** in its summary, or select the `nia-VERSION-dev-RUN.ATTEMPT-COMMIT` entry under **Artifacts**.
5. Extract the ZIP on your phone and install the APK. Select **nia Dev** as your Home app while testing; select **nia** again to return to the regular launcher.

You must be signed into GitHub to download an artifact. Downloads expire after **30 days**. This workflow creates neither a tag nor a GitHub release, and its token has read-only repository access. Branch selection becomes available once the workflow is on `main`; choose a branch that contains the dev workflow and build configuration.

### Installation and settings

**nia Dev** uses `dev.robinsiegl.nia.dev`, so it can sit alongside the normal launcher. Its settings and setup progress are independent. Export a backup from nia and import it into nia Dev to copy your current layout, colors and assignments. Changing the default Home app does not uninstall either app.

Each dev download uses the existing signing secrets, so a newer dev APK updates the same installation and retains its settings. The app inherits production minification/resource shrinking and is not debuggable. CI validates an unsigned dev package; the manual workflow is the signed download channel.

For example, a base version of `1.5.0`, workflow run `42`, attempt `1` and commit `aea9190` produce:

- APK: `nia-1.5.0-dev-42.1-aea9190.apk`
- Installed version/About: `1.5.0-dev.42.1+aea9190`
- Android version code: `1005000`, taken from Gradle.

Repeat builds can update the same Android version code. A branch with a lower base code cannot replace a newer dev installation; see Troubleshooting below. The production version keeps its exact committed name and code.

### Download contents and verification

The bundle contains the signed APK, `SHA256SUMS.txt` and `BUILD_INFO.json`. Build information records the app/version, full source commit, branch, workflow run, APK checksum and public signing-certificate fingerprint. It contains no keystore or signing credentials.

Before upload, the workflow verifies the signature and rejects an APK with the wrong app ID, version, label or debuggable flag. The summary links directly to that run's artifact and source commit. About identifies dev builds and explains that its update links point to public production releases.

See the [native About example](screenshots/about-dev.png) for the dev identity and update wording.

## Trigger a production build

1. Commit the prepared version and changes to `main`.
2. Open **[Actions → Production APK → Run workflow](https://github.com/robin-siegl/Nia/actions/workflows/production.yml)** and select `main`.
3. Leave **publish_release** enabled for a public release, or disable it for a signed artifact-only build. There are no version inputs.
4. Select **Run workflow**. After it succeeds, download `nia-VERSION.apk` and `SHA256SUMS.txt` from [Releases](https://github.com/robin-siegl/Nia/releases). For an artifact-only run, download and extract **nia-production-VERSION** from that run.

The APK, tag and release use the exact committed Gradle version. The workflow does not increment it, select it from tags or accept a command-line override. Version validation, tests, lint, minification and APK signature verification run before publication.

Publication creates `vVERSION` atomically at the exact source commit, then attaches the verified files with generated release notes. Failed checks do not create a tag. Artifact-only builds create neither a tag nor a public release, but still require a valid version newer than existing stable tags.

The workflow restores the existing production key, masks signing values and removes its temporary keystore afterward. It never generates a replacement key during a production build. Agents should prepare a release-ready change but trigger this workflow only when requested.

## Local signed release

Set `NIA_KEYSTORE_PATH`, `NIA_STORE_PASSWORD`, `NIA_KEY_ALIAS` and `NIA_KEY_PASSWORD` in your local process environment, then run:

```sh
./gradlew :app:assembleRelease :app:lintRelease
```

The signed output is `app/build/outputs/apk/release/app-release.apk`. Without signing variables, the local release APK is unsigned and cannot be installed directly. GitHub's production workflow configures these variables for you.

## Before publishing

Complete the relevant [device checks](DEVICE_TESTING.md), record the actual OS build, and install over an earlier release signed with the same key. Confirm that settings and app assignments survive. Check the APK's version, signing identity and checksum.

Keep the application ID and production key stable. Debug APKs have a separate ID and signing identity and are not the production update channel. A source license still needs to be selected before presenting nia as open source or assigning license terms to contributions.

## Troubleshooting

- **Missing secrets:** Check the four names above. Use repository secrets rather than variables.
- **Invalid base64:** Encode the complete keystore file, not its path or filename.
- **Wrong password or alias:** Use credentials from the same keystore. The alias need not match the app name.
- **No public download:** Enable **publish_release**, wait for success, then open Releases. Artifact-only builds appear in the workflow run.
- **Version already exists or is too old:** Prepare a newer version and matching code, commit it to `main`, and rerun. Existing tags are never replaced.
- **Older dev APK will not install:** Android blocks a lower version code. Use a branch with the same or newer base version, or uninstall only nia Dev after exporting its settings, then install and restore. Regular nia is a separate installation.
- **Dev download expired:** Run Dev APK again for a new 30-day artifact. Dev builds can reuse an already-released base version.
- **Code mismatch:** Apply the formula in the version policy and update both properties together.
- **Failure before tag creation:** Fix the failure and retry the same prepared version if no tag was created.
- **Tag created but publication failed:** Use that run's verified APK/checksum artifact to finish publishing the existing tag. Keep it on its original commit; do not rebuild a different APK under that version.
