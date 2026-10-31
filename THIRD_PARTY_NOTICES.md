# Third-party notices

## Color generation

The OKLab conversion matrices in [Palette.kt](app/src/main/java/dev/robinsiegl/nia/theme/Palette.kt) are based on [Björn Ottosson's public-domain implementation](https://bottosson.github.io/posts/oklab/). Gamut mapping, tone selection and contrast checks are nia's own palette logic.

## Android and build dependencies

nia uses Android, AndroidX/Jetpack, Kotlin, Kotlin coroutines and Kotlin serialization through their published artifacts and licenses. Dependency source and license information is available from the corresponding upstream projects and Maven artifacts. The Gradle wrapper retains the license headers from its verified upstream distribution.

## Shortcut and interface icons

The bundled icons are **Material Symbols Outlined by Google**, under the Apache License 2.0. nia uses the unfilled, weight-300, grade-0, 24px optical-size variants from [google/material-design-icons](https://github.com/google/material-design-icons), pinned to commit `10ecc14790612ca8fd164e3f4ffe9c16fc50d5b7`.

The complete [Apache 2.0 license](app/src/main/assets/licenses/material-symbols-Apache-2.0.txt) is bundled in the APK. Each vector retains its source and modification notice: the paths are unchanged, and the Android theme tint is removed so nia can apply its shared foreground color. The upstream repository has no separate NOTICE file.

The subset and checksums are recorded in [material_symbols.json](scripts/material_symbols.json). [sync_material_symbols.py](scripts/sync_material_symbols.py) imports the pinned Android vectors and generates the stable icon catalog/resource mapping. Normal builds and the picker use bundled assets and make no icon-download requests.

## Clock fonts

`nia_header_regular.ttf` and `nia_header_semibold.ttf` are renamed static subsets of Google Sans, Copyright 2025 The Google Sans Project Authors ([upstream project](https://github.com/googlefonts/googlesans)). The source is the official [Google Fonts distribution](https://github.com/google/fonts/tree/main/ofl/googlesans), Git blob `a5e76cc713206f1cfe9a7fc31cc1b6f035034d81`.

The fonts retain the SIL Open Font License 1.1 and their original copyright/license metadata. The complete [license](app/src/main/assets/licenses/google-sans-OFL.txt) and [trademark notice](app/src/main/assets/licenses/google-sans-TRADEMARKS.md) are bundled in the APK. The derivatives use the name **Nia Header**. Google does not sponsor or endorse nia.

[build_header_fonts.py](scripts/build_header_fonts.py) reproduces the committed subsets from the verified upstream TTF using FontTools.
