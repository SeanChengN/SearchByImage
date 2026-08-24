import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val releaseKeystorePath = providers.environmentVariable("SEARCHBYIMAGE_KEYSTORE_PATH").orNull
val releaseStorePassword = providers.environmentVariable("SEARCHBYIMAGE_STORE_PASSWORD").orNull
val releaseKeyAlias = providers.environmentVariable("SEARCHBYIMAGE_KEY_ALIAS").orNull
val releaseKeyPassword = providers.environmentVariable("SEARCHBYIMAGE_KEY_PASSWORD").orNull
val releaseSigningValues = listOf(
    releaseKeystorePath,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword,
)
val releaseSigningConfigured = releaseSigningValues.all { !it.isNullOrBlank() }
check(releaseSigningValues.none { !it.isNullOrBlank() } || releaseSigningConfigured) {
    "Release signing environment variables must either all be set or all be absent"
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

android {
    namespace = "io.github.seancheng.searchbyimage"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.seancheng.searchbyimage"
        minSdk = 29
        targetSdk = 36
        versionCode = 30002
        versionName = "3.0.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }

    signingConfigs {
        if (releaseSigningConfigured) {
            create("release") {
                storeFile = file(requireNotNull(releaseKeystorePath))
                storePassword = requireNotNull(releaseStorePassword)
                keyAlias = requireNotNull(releaseKeyAlias)
                keyPassword = requireNotNull(releaseKeyPassword)
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            if (releaseSigningConfigured) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    packaging {
        resources.excludes += setOf(
            "/META-INF/{AL2.0,LGPL2.1}",
            "/META-INF/LICENSE.md",
            "/META-INF/LICENSE-notice.md",
        )
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation(libs.core.ktx)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.navigation3.runtime)
    implementation(libs.navigation3.ui)
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.icons)
    implementation(libs.compose.tooling.preview)
    implementation(libs.browser)
    implementation(libs.work.runtime)
    implementation(libs.datastore)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation(libs.exifinterface)
    implementation(libs.cropper)
    implementation(platform(libs.okhttp.bom))
    implementation(libs.okhttp)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.coroutines.android)

    ksp(libs.room.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    testImplementation(platform(libs.okhttp.bom))
    testImplementation(libs.mockwebserver)
    testImplementation(libs.room.testing)
    testImplementation(libs.work.testing)
    testImplementation(libs.json)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.compose.test.junit4)
    androidTestImplementation(libs.room.testing)
    debugImplementation(libs.compose.tooling)
    debugImplementation(libs.compose.test.manifest)
}
