plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.secrets)
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "dev.dzianisv.convert264"
    minSdk = 24
    targetSdk = 36
    versionCode = 18
    versionName = "0.2.7"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  // WL-35 (F-Droid): two store flavors. `play` is the Play Store
  // build (Play in-app review included); `foss` is the F-Droid
  // build — same app, same applicationId, no non-free libraries.
  // The review prompt lives behind ReviewLauncher so the foss
  // variant compiles without review-ktx.
  flavorDimensions += "store"
  productFlavors {
    create("play") { dimension = "store" }
    create("foss") { dimension = "store" }
  }

  // Do not remove or modify these signingConfigs. They are necessary for building, installing,
  // and updating Android apps in AI Studio.
  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD")
      keyAlias = "upload"
      keyPassword = System.getenv("KEY_PASSWORD")
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      // FFmpegKit ships native libs for arm64-v8a + x86_64 only
      // (the maintained fork publishes no 32-bit ABIs); the
      // release APK only needs the ARM64 ABI (sideloaded to
      // phones, incl. Pixel).
      ndk { abiFilters += "arm64-v8a" }
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      // Do not remove or modify this signingConfig assignment. It is necessary for Android apps in
      // AI Studio.
      signingConfig = signingConfigs.getByName("release")
    }
    debug {
      // Emulator smoke builds run on x86_64 only — packaging the
      // ARM FFmpeg .so files as well would triple the debug APK
      // for no benefit.
      ndk { abiFilters += "x86_64" }
      // Do not remove or modify this signingConfig assignment. It is necessary for Android apps in
      // AI Studio.
      signingConfig = signingConfigs.getByName("debugConfig")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
  }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
}

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  // Play in-app review (v0.2.1): the feedback loop for the Play
  // Store launch — prompt once, after the 3rd successful conversion.
  // Play flavor only (WL-35): the foss flavor must not link any
  // non-free library, so review-ktx is excluded there and
  // ReviewLauncher is a no-op in foss. Kotlin DSL has no
  // playImplementation accessor — the string-invoke form (same as
  // "ksp" below) is the way to address a flavor configuration.
  "playImplementation"(libs.play.review.ktx)
  // Play Billing, one-time Pro unlock (v0.2.6, WL-14): same
  // flavor rule as review-ktx — the foss flavor must not link
  // any non-free library, so billing-ktx is play-only and the
  // billing client lives behind ProBillingProvider.
  "playImplementation"(libs.play.billing.ktx)
  implementation(libs.androidx.media3.transformer)
  implementation(libs.androidx.media3.effect)
  implementation(libs.androidx.media3.common)
  implementation(libs.androidx.media3.exoplayer)
  // FFmpeg engine for the universal converter (v0.2.0 spike):
  // full-gpl build = libx264, libx265, libvpx, libaom, libmp3lame,
  // libopus, libvorbis — the codecs Media3 Transformer cannot do.
  // NOTE: upstream com.arthenica:ffmpeg-kit was retired Jan 2025
  // and its binaries were REMOVED from Maven Central Apr 1, 2025
  // (Gradle: "Could not find com.arthenica:ffmpeg-kit-full-gpl:6.0").
  // The maintained fork dev.ffmpegkit-maintained republishes the
  // same artifacts under the same package (com.arthenica.ffmpegkit)
  // and API — only the group ID changed. 8.1.7 = FFmpeg 8.1 LTS.
  implementation(libs.ffmpeg.kit.full.gpl)
  // FFmpegKit needs smart-exception-java at RUNTIME
  // (FFmpegKitConfig.<clinit> references
  // com.arthenica.smartexception.java.Exceptions), but the fork's
  // published 8.1.7 POM declares NO dependencies at all — the
  // library's own build.gradle declares it as api, and the fork
  // itself fixed the POM later (commit 8d05f55), but 8.1.7 ships
  // without it. Without this line every FFmpegKit call dies with
  // NoClassDefFoundError (killed spike run 42887700284). The
  // artifact is still on Maven Central (verified HTTP 200).
  implementation(libs.smart.exception.java)
  // implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  // implementation(libs.coil.compose)
  // Uncomment to use Firestore:
  // implementation(libs.firebase.firestore)

  // Uncomment ALL FOUR of the following dependencies together to use Firebase Auth and Google
  // Sign-In via Credential Manager:
  // implementation(libs.firebase.auth)
  // implementation(libs.androidx.credentials)
  // implementation(libs.androidx.credentials.play.services)
  // implementation(libs.googleid)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  // implementation(libs.play.services.location)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
}
