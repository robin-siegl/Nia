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

## Trigger a production build

1. Open **[Actions → Production APK → Run workflow](https://github.com/robin-siegl/Nia/actions/workflows/production.yml)** on `main`.
2. Optionally enter a version such as `0.1.0` or `0.2.0-beta.1`. Empty uses `nia.versionName` from `gradle.properties`.
3. Leave **publish_release** disabled for an artifact-only build, or enable it for a public GitHub release.
4. The workflow runs tests and lint, builds and shrinks the release, verifies its APK signature, and uploads **nia-production-VERSION**.
5. Download/extract that artifact. Install `nia-VERSION.apk`. `SHA256SUMS.txt` records its checksum.

With **publish_release** enabled, the APK and checksum are also available under **[Releases](https://github.com/robin-siegl/Nia/releases)** for public download. No artifact extraction or GitHub sign-in is needed for those release assets.

The release version code is `100 + GITHUB_RUN_NUMBER`. This remains increasing across normal runs of this workflow. The public tag is `vVERSION`; select a new version when publishing a new GitHub release. Publishing to an existing tag intentionally fails rather than replacing an existing release's APK.

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
- **No public download:** Enable **publish_release** when running the workflow, then open **Releases** after it succeeds.
- **Release tag already exists:** Use a new version. The workflow does not replace published APKs.
