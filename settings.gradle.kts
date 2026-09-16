enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

// Build the KMPMedia library straight from local source (always the latest),
// substituting the published coordinate with the sibling repo's :library project.
// No GitHub Packages / GPR credentials needed.
includeBuild("../KMPMedia") {
    dependencySubstitution {
        substitute(module("com.solidkey:kmpmedia-lib")).using(project(":library"))
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

rootProject.name = "KMPMediaDemo"
include(":androidApp")
include(":shared")
