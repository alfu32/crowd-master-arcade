plugins {
    kotlin("jvm")
    id("org.jetbrains.intellij.platform")
}

val explicitLocalIdePath = providers.gradleProperty("intellijPlatformLocalPath")
    .orElse(providers.environmentVariable("INTELLIJ_PLATFORM_LOCAL_PATH"))
    .orNull

val toolboxIdePath = listOf(
    "intellij-idea-ultimate",
    "intellij-idea-community"
)
    .map { file("${System.getProperty("user.home")}/.local/share/JetBrains/Toolbox/apps/$it") }
    .firstOrNull { it.isDirectory }
    ?.absolutePath

val localIdePath = explicitLocalIdePath ?: toolboxIdePath

dependencies {
    intellijPlatform {
        if (localIdePath != null) {
            local(localIdePath)
        } else {
            intellijIdeaCommunity("2024.3.6")
        }
    }
}

intellijPlatform {
    pluginConfiguration {
        id = "com.crowdmasterarcade.levels"
        name = "Crowd Master Arcade Level Support"
        version = project.version.toString()
        description = "Syntax highlighting for Crowd Master Arcade .level and .cma-level files."
        ideaVersion {
            sinceBuild = "243"
        }
    }
}

kotlin {
    jvmToolchain(21)
}

tasks.withType<Test>().configureEach {
    enabled = false
}
