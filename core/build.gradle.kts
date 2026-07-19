import org.gradle.language.jvm.tasks.ProcessResources

plugins {
    kotlin("jvm")
}

val gdxVersion: String by project
val packagedAssetsDir = layout.projectDirectory.dir("src/main/resources/assets")

dependencies {
    api("com.badlogicgames.gdx:gdx:$gdxVersion")
    api("com.kotcrab.vis:vis-ui:1.5.3")

    testImplementation(kotlin("test"))
}

val generateAssetIndex by tasks.registering {
    val assetsDir = packagedAssetsDir.asFile
    val indexFile = packagedAssetsDir.file("index.txt").asFile
    inputs.files(fileTree(assetsDir) { exclude("index.txt", "**/*.bak") })
    outputs.file(indexFile)
    doLast {
        val entries = assetsDir
            .walkTopDown()
            .filter { it.isFile }
            .map { it.relativeTo(assetsDir).invariantSeparatorsPath }
            .filter { it != "index.txt" && !it.endsWith(".bak", ignoreCase = true) }
            .sorted()
            .toList()
        indexFile.parentFile.mkdirs()
        indexFile.writeText(entries.joinToString(separator = "\n", postfix = "\n"))
    }
}

tasks.named<ProcessResources>("processResources") {
    dependsOn(generateAssetIndex)
    exclude("**/*.bak")
}

tasks.test {
    useJUnitPlatform()
}
