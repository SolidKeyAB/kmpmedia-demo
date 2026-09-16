import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
    jvmToolchain(17)

    val xcf = XCFramework("shared")
    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "shared"
            isStatic = true
            xcf.add(this)
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(libs.animation)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kermit)
            // Substituted with the local ../KMPMedia :library project (see settings.gradle.kts)
            implementation("com.solidkey:kmpmedia-lib")
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
        }
    }
}

android {
    namespace = "com.solidkey.shared"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// Assemble the iOS XCFramework and copy it where the Xcode project links it from.
tasks.register("assembleSharedXCFrameworkAndCopy") {
    dependsOn("assembleSharedReleaseXCFramework")
    doLast {
        val frameworkDir = layout.buildDirectory.get().asFile
            .resolve("XCFrameworks/release/shared.xcframework")
        val destinationDir = rootProject.projectDir.resolve("iosApp/xcframeworks")
        destinationDir.mkdirs()
        frameworkDir.copyRecursively(destinationDir.resolve("shared.xcframework"), overwrite = true)
    }
}
