import org.jetbrains.intellij.platform.gradle.extensions.intellijPlatform

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}

plugins {
    id("org.jetbrains.intellij.platform.settings") version "2.17.0"
}

val buildsIntellijPlugin = gradle.startParameter.taskNames.any { taskName ->
    taskName == "test" ||
        taskName == "check" ||
        taskName.endsWith(":test") ||
        taskName.endsWith(":check") ||
        taskName.contains("level-intellij-plugin") ||
        taskName.contains("buildPlugin")
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        google()
        if (buildsIntellijPlugin) {
            intellijPlatform {
                defaultRepositories()
            }
        }
    }
}

rootProject.name = "crowd-master-arcade"
include("core", "lwjgl3", "android", "level-intellij-plugin")
