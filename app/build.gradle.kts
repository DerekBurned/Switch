import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.objectbox)
    alias(libs.plugins.kover)
}

android {
    namespace = "com.example.messenger"
    compileSdk = 37

    val agoraAppId: String = run {
        val props = Properties()
        rootProject.file("local.properties").takeIf { it.exists() }
            ?.inputStream()?.use { props.load(it) }
        props.getProperty("AGORA_APP_ID")
            ?: (project.findProperty("AGORA_APP_ID") as String?)
            ?: ""
    }

    val featuresOffInRelease = listOf(
        "profile_edit_change_photo",
        "accounts_switch",
        "partner_profile_mute",
        "partner_profile_block",
        "partner_profile_shared_media",
        "settings_privacy_controls",
        "settings_security",
        "settings_notification_controls",
        "settings_data_storage",
        "settings_language",
    )

    defaultConfig {
        applicationId = "com.example.messenger"
        minSdk = 29
        targetSdk = 36
        versionCode = providers.gradleProperty("VERSION_CODE").get().toInt()
        versionName = providers.gradleProperty("VERSION_NAME").get()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        buildConfigField("String", "AGORA_APP_ID", "\"$agoraAppId\"")
        buildConfigField("String", "FEATURES_OFF_IN_RELEASE", "\"${featuresOffInRelease.joinToString(",")}\"")


    }

    buildTypes {
        debug {
            manifestPlaceholders["crashlyticsCollectionEnabled"] = false
            buildConfigField("String", "FEATURES_OFF_BY_DEFAULT", "\"\"")
        }
        release {
            manifestPlaceholders["crashlyticsCollectionEnabled"] = true
            buildConfigField("String", "FEATURES_OFF_BY_DEFAULT", "\"${featuresOffInRelease.joinToString(",")}\"")
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlin{
        target{
            compilerOptions {
                optIn.add("kotlin.RequiresOptIn")
                freeCompilerArgs.add("-XXLanguage:+PropertyParamAnnotationDefaultTargetMode")
            }
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        jniLibs {
            useLegacyPackaging = true
        }
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all { test ->
                test.useJUnitPlatform()
                test.maxHeapSize = "512m"
                test.maxParallelForks = 1
                test.inputs.file(rootProject.file("remoteconfig.template.json"))
                    .withPropertyName("remoteConfigTemplate")
                    .withPathSensitivity(PathSensitivity.RELATIVE)
            }
        }
    }
}

configurations.all {
    resolutionStrategy {
        force("androidx.concurrent:concurrent-futures:1.2.0")
        force("androidx.concurrent:concurrent-futures-ktx:1.2.0")
        // Media libs (coil3/media3/camerax) transitively pull kotlin-stdlib 2.4.0, whose
        // metadata version (2.4.0) the Kotlin 2.3.10 toolchain + Hilt processor can't read.
        // Pin stdlib to the compiler version.
        force("org.jetbrains.kotlin:kotlin-stdlib:${libs.versions.kotlin.get()}")
        force("org.jetbrains.kotlin:kotlin-stdlib-jdk8:${libs.versions.kotlin.get()}")
        force("org.jetbrains.kotlin:kotlin-stdlib-jdk7:${libs.versions.kotlin.get()}")
    }
}

dependencies {
    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.runtime.saveable)

    // Firebase - BOM manages all versions
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.database)
    implementation(libs.firebase.functions)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.config)

    // Hilt - using KSP
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.work)
    // Required so @HiltWorker (e.g. SyncWorker) is registered in HiltWorkerFactory.
    ksp(libs.androidx.hilt.compiler)

    // Navigation 3
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.kotlinx.serialization.json)

    // DataStore - theme-mode preference
    implementation(libs.androidx.datastore.preferences)

    // Splash screen (animated logo on cold start)
    implementation(libs.androidx.core.splashscreen)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)

    // Lifecycle
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.process)

    // Gson
    implementation(libs.google.gson)

    // Tink (E2EE crypto primitives)
    implementation(libs.tink.android)

    //AgoraRTC
    implementation("io.agora.rtc:full-sdk:4.6.3")

    // Media3 - ExoPlayer
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui)

    //Coil
    implementation(libs.coil.compose)
    implementation(libs.coil.video)
    implementation(libs.coil.network.okhttp)

    // CameraX
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.camera.video)

    // Testing — JUnit5 (Jupiter) + AssertK + Turbine + MockK + coroutines-test
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.jupiter.params)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.assertk)
    testImplementation(libs.turbine)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testRuntimeOnly(libs.junit.vintage.engine)
    testImplementation(libs.junit)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

kover {
    reports {
        filters {
            excludes {
                classes(
                    "*_",
                    "*.MyObjectBox",
                    "*Cursor",
                    "*_Factory",
                    "*_MembersInjector",
                    "*_HiltModules*",
                    "*.Hilt_*",
                    "*ComposableSingletons*",
                    "*.BuildConfig",
                    "hilt_aggregated_deps.*",
                    "dagger.hilt.internal.*",
                )
                packages("com.example.messenger.di")
            }
        }
        verify {
            rule {
                minBound(28)
            }
        }
    }
}