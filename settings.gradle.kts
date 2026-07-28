pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "legolas"

val locationEnabled = providers.gradleProperty("locationEnabled")
    .orElse("true")
    .get()
    .toBooleanStrict()

include(":app")
if (locationEnabled) include(":modules:location")
