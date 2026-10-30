# Release nia

## One-time signing setup

1. Clone this repository. Install JDK 17+ or Android Studio, Python 3, and optionally GitHub CLI.
2. Sign into GitHub CLI with `gh auth login` if using automatic secret upload.
3. Run `python scripts/setup_signing.py --upload` from the repository. Windows users may run `.\scripts\setup-signing.ps1 --upload`.
4. Privately back up the resulting `.signing/` folder. It contains your release key and its credentials. The helper reuses an existing bundle and refuses to silently overwrite an existing key.

Without GitHub CLI, omit `--upload`. Open the generated `.signing/nia-signing.json` locally, copy its entire contents, and add a repository Actions secret named **NIA_SIGNING_BUNDLE**. The JSON contains the base64 keystore, store password, key alias and key password. Do not commit it or attach it to an issue/release.

The helper also looks for `keytool` under `JAVA_HOME` and Android Studio's normal installation path, addressing Windows machines where `keytool` is not on PATH.

## Trigger a production build

1. Open **Actions → Production APK → Run workflow** on `main`.
2. Optionally enter a version such as `0.1.0` or `0.2.0-beta.1`. Empty uses `nia.versionName` from `gradle.properties`.
3. Leave **publish_release** disabled for an artifact-only build, or enable it for a public GitHub release.
4. The workflow runs tests and lint, builds and shrinks the release, verifies its APK signature, and uploads **nia-production-VERSION**.
5. Download/extract that artifact. Install `nia-VERSION.apk`. `SHA256SUMS.txt` records its checksum.

The release version code is `100 + GITHUB_RUN_NUMBER`. This remains increasing across normal runs of this workflow. The public tag is `vVERSION`; select a new version when publishing a new GitHub release. Publishing to an existing tag intentionally fails rather than replacing an existing release's APK.

The workflow uses repository secrets only in the signed-build job, masks decoded signing values, and deletes the temporary keystore afterward. It never creates a new production signing key during an APK build. Missing or invalid signing configuration gives a setup error.

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
