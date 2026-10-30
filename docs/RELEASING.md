# Release nia

nia builds signed APKs through GitHub Actions. No local Gradle build or Git tag is needed to publish a release.

## One-time signing setup

**If you already added the four `ANDROID_*` secrets for nia, skip to [Trigger a production build](#trigger-a-production-build).** These are the same secret names used by [pip-os](https://github.com/robin-siegl/pip-os/blob/main/docs/RELEASING.md). No signing-bundle secret is required.

Production updates require the same signing key every time. Keep a private backup of the keystore, alias and passwords. Keep the original keystore outside the Git repository.

Generate a keystore with a locally installed JDK:

```sh
keytool -genkeypair -v -keystore nia-release.jks -alias nia -keyalg RSA -keysize 3072 -validity 10000
```

If PowerShell cannot find `keytool` and Android Studio is installed in its usual location, use its bundled tool:

```powershell
& 'C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe' -genkeypair -v -keystore nia-release.jks -alias nia -keyalg RSA -keysize 3072 -validity 10000
```

Store these repository secrets under **Settings → Secrets and variables → Actions → New repository secret**:

| Secret | Value |
| --- | --- |
| `ANDROID_KEYSTORE_BASE64` | Base64 of the whole nia keystore, on one line |
| `ANDROID_KEYSTORE_PASSWORD` | Keystore password |
| `ANDROID_KEY_ALIAS` | `nia`, or the alias you created |
| `ANDROID_KEY_PASSWORD` | Private-key password; use the keystore password if they are the same |

Encode the file in Windows PowerShell and copy it to the clipboard:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes((Resolve-Path ./nia-release.jks))) | Set-Clipboard
```

On Linux use `base64 -w 0 nia-release.jks`; on macOS use `base64 -i nia-release.jks | tr -d '\n'`. Paste that output as `ANDROID_KEYSTORE_BASE64`.

### Optional helper for a new signing key

For a fresh setup, `python scripts/setup_signing.py --upload` creates a key and uploads the same four secrets using an authenticated GitHub CLI. Windows users may use `.\scripts\setup-signing.ps1 --upload`. It finds `keytool` through PATH, `JAVA_HOME` or Android Studio, and reuses its existing `.signing/` backup on subsequent runs.

Skip this helper if you have already created nia's key and configured its secrets. Back up `.signing/` privately if you use it. Without `--upload`, the helper saves a local JSON backup and prints which field maps to each of the four secrets; it does not print credentials.

## Prepare the project version

The committed `gradle.properties` is the source of the production version. Update both values with the release's changes and commit them to `main`:

```properties
nia.versionName=1.1.1
nia.versionCode=1001001
```

The production workflow builds these exact values. It does not choose a version from Git tags, increment it or accept a version override. For example, a future `1.2.0` release uses `nia.versionName=1.2.0` and `nia.versionCode=1002000`.

## Trigger a production build

1. Open **[Actions → Production APK → Run workflow](https://github.com/robin-siegl/Nia/actions/workflows/production.yml)** on `main`.
2. Leave **publish_release** enabled for the public APK download. Disable it only for an artifact-only build. There are no version fields to fill in.
3. Select **Run workflow**. The workflow validates the committed project version, runs tests and lint, builds and shrinks the release, verifies its APK signature, and uploads **nia-production-VERSION**.
4. Open **Releases** and download `nia-VERSION.apk`. `SHA256SUMS.txt` records its checksum. Artifact-only builds are downloaded/extracted from the workflow run instead.

With **publish_release** enabled, the APK and checksum are also available under **[Releases](https://github.com/robin-siegl/Nia/releases)** for public download. No artifact extraction or GitHub sign-in is needed for those release assets.

**Current project version:** `1.1.1`. A production build creates `nia-1.1.1.apk` and, when publication is enabled, the tag/release `v1.1.1`. After that version is published, prepare and commit a newer project version before another production build.

The latest stable `vMAJOR.MINOR.PATCH` Git tag is used only to reject duplicate or older versions. Tags are compared numerically; prerelease and unrelated tags are ignored. Leading zeroes, prerelease suffixes, existing versions and builds from a branch other than `main` are rejected. These checks also apply to artifact-only production builds. If a run fails before creating its tag, the same project version can be retried.

Android `versionCode` must equal `major * 1000000 + minor * 1000 + patch`; the workflow checks the committed value and stops with an actionable error if it does not match. `1.0.0` uses `1000000`, above the MVP run-number codes. Major is limited to 1999, minor and patch to 999, keeping codes positive and below Android's limit. Increasing supported versions produces increasing version codes independently of workflow run numbers.

After build verification, the publish job atomically creates `vVERSION` at the exact source commit, rejecting an existing tag. It then publishes the APK and checksum with generated release notes. No tag is created by failed tests, lint, builds or signature verification. Artifact-only builds do not create tags.

The workflow restores your existing keystore, masks signing values, verifies the APK signature, and deletes the temporary keystore afterward. It never generates a new production key during an APK build.

## Local signed release

Provide `NIA_KEYSTORE_PATH`, `NIA_STORE_PASSWORD`, `NIA_KEY_ALIAS` and `NIA_KEY_PASSWORD` in your local process environment, then run:

```sh
./gradlew :app:assembleRelease :app:lintRelease
```

The output is `app/build/outputs/apk/release/app-release.apk`. A release built without signing environment variables is unsigned and cannot be installed directly. The GitHub production workflow handles these variables for you.

## Before publishing

- Complete the Pixel acceptance checklist and record the actual Android version/build.
- Install over an earlier release signed with the same key. Confirm assignments and settings survive.
- Check the release version/name and checksum. Keep the application ID and release key stable for updates.
- Choose the source license before describing nia as open source or accepting external contributions under a particular license.

Debug installs are separate apps and use development signing. They are suitable for testing but are not the production update channel.

## Troubleshooting

- **Missing repository Actions secrets:** Check all four names in the table above. Store them as repository secrets, not variables. No additional bundle secret is needed.
- **Invalid base64:** Encode the complete keystore file, rather than its path or filename.
- **Wrong password or alias:** Use the values from the same keystore you encoded. The alias does not need to match the app name.
- **No public download:** Keep **publish_release** enabled when running the workflow, then open **Releases** after it succeeds.
- **Release version already exists or is too old:** Update `nia.versionName` and its matching `nia.versionCode` in `gradle.properties`, commit the change to `main`, then run the workflow again. The workflow never invents or increments a version.
- **Android version code does not match:** Use `major * 1000000 + minor * 1000 + patch` for the committed `nia.versionName`; for `1.2.0`, set `nia.versionCode=1002000`.
- **Tag exists but publishing failed:** Download the verified APK/checksum artifact from that run and finish creating the GitHub Release for its existing tag. Keep the tag at its original commit; do not move it or rebuild a different APK under that version.
