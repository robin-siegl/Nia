plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

val signingPath = providers.environmentVariable("NIA_KEYSTORE_PATH")
val devBuildId = providers.environmentVariable("NIA_DEV_BUILD_ID").getOrElse("local")
val devCommit = providers.environmentVariable("NIA_DEV_COMMIT").getOrElse("local")
require((devBuildId == "local" && devCommit == "local") ||
    (devBuildId.matches(Regex("[1-9][0-9]*\\.[1-9][0-9]*")) && devCommit.matches(Regex("[0-9a-f]{7}")))) {
    "Dev build metadata must contain a run.attempt number and a seven-character commit SHA."
}
android {
    namespace = "dev.robinsiegl.nia"
    compileSdk = 36
    defaultConfig {
        applicationId = "dev.robinsiegl.nia"
        minSdk = 29
        targetSdk = 36
        versionCode = providers.gradleProperty("nia.versionCode").get().toInt()
        versionName = providers.gradleProperty("nia.versionName").get()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    signingConfigs {
        if (signingPath.isPresent) create("production") {
            storeFile = file(signingPath.get())
            storePassword = providers.environmentVariable("NIA_STORE_PASSWORD").get()
            keyAlias = providers.environmentVariable("NIA_KEY_ALIAS").get()
            keyPassword = providers.environmentVariable("NIA_KEY_PASSWORD").get()
        }
    }
    buildTypes {
        debug { applicationIdSuffix = ".debug"; versionNameSuffix = "-debug" }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            if (signingPath.isPresent) signingConfig = signingConfigs.getByName("production")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        create("dev") {
            initWith(getByName("release"))
            applicationIdSuffix = ".dev"
            versionNameSuffix = if (devBuildId == "local") "-dev" else "-dev.$devBuildId+$devCommit"
            matchingFallbacks += "release"
        }
    }
    buildFeatures { compose = true; buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    testOptions { unitTests.isIncludeAndroidResources = true }
    lint { abortOnError = true; checkReleaseBuilds = true }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.10.00"))
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")
    implementation("androidx.datastore:datastore-preferences:1.1.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    testImplementation("org.robolectric:robolectric:4.16.1")
    testImplementation(platform("androidx.compose:compose-bom:2025.10.00"))
    testImplementation("androidx.compose.ui:ui-test-junit4")
    testImplementation("androidx.test:core:1.7.0")
}
