# Third-party notices

The OKLab conversion matrices in `Palette.kt` are based on Björn Ottosson's public-domain implementation: https://bottosson.github.io/posts/oklab/. The gamut mapping, tone selection and contrast checks are nia's palette logic.

Android, AndroidX/Jetpack, Kotlin, Kotlin coroutines and Kotlin serialization are used through their published artifacts and licenses. Gradle's wrapper is generated from the verified Gradle distribution and retains its upstream license headers. Dependency source and license information is available in the corresponding upstream projects and Maven artifacts.

## Clock typography

The `nia_header_regular.ttf` and `nia_header_semibold.ttf` fonts are renamed static subsets of Google Sans, Copyright 2025 The Google Sans Project Authors (https://github.com/googlefonts/googlesans). The source is the official [Google Fonts distribution](https://github.com/google/fonts/tree/main/ofl/googlesans), Git blob `a5e76cc713206f1cfe9a7fc31cc1b6f035034d81`.

The fonts retain the SIL Open Font License 1.1 and their original copyright/license metadata. The complete [license](app/src/main/assets/licenses/google-sans-OFL.txt) and [trademark notice](app/src/main/assets/licenses/google-sans-TRADEMARKS.md) are also bundled in the APK. The derivatives are named Nia Header; Google does not sponsor or endorse nia. `scripts/build_header_fonts.py` reproduces the committed subsets from the verified upstream TTF with FontTools.
