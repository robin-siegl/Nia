# Third-party notices

## Color generation

The OKLab conversion matrices in [Palette.kt](app/src/main/java/dev/robinsiegl/nia/theme/Palette.kt) are based on [Björn Ottosson's public-domain implementation](https://bottosson.github.io/posts/oklab/). Gamut mapping, tone selection and contrast checks are nia's own palette logic.

## Android and build dependencies

nia uses Android, AndroidX/Jetpack, Kotlin, Kotlin coroutines and Kotlin serialization through their published artifacts and licenses. Dependency source and license information is available from the corresponding upstream projects and Maven artifacts. The Gradle wrapper retains the license headers from its verified upstream distribution.

## Clock fonts

`nia_header_regular.ttf` and `nia_header_semibold.ttf` are renamed static subsets of Google Sans, Copyright 2025 The Google Sans Project Authors ([upstream project](https://github.com/googlefonts/googlesans)). The source is the official [Google Fonts distribution](https://github.com/google/fonts/tree/main/ofl/googlesans), Git blob `a5e76cc713206f1cfe9a7fc31cc1b6f035034d81`.

The fonts retain the SIL Open Font License 1.1 and their original copyright/license metadata. The complete [license](app/src/main/assets/licenses/google-sans-OFL.txt) and [trademark notice](app/src/main/assets/licenses/google-sans-TRADEMARKS.md) are bundled in the APK. The derivatives use the name **Nia Header**. Google does not sponsor or endorse nia.

[build_header_fonts.py](scripts/build_header_fonts.py) reproduces the committed subsets from the verified upstream TTF using FontTools.
