plugins {
    kotlin("jvm")
    application
}

import org.gradle.api.tasks.bundling.Compression
import org.gradle.jvm.tasks.Jar

val gdxVersion: String by project
val appSlug = "crowd-master-arcade"
val appDisplayName = "Crowd Master Arcade"
val bundleVersion = project.version.toString()
val distDir = rootProject.layout.projectDirectory.dir("dist")
val launcherDir = layout.buildDirectory.dir("generated/launchers")
val jdkCacheDir = layout.buildDirectory.dir("jdk-cache")
val bundleJdkVersion = (findProperty("bundleJdkVersion") ?: "21.0.9+10").toString()
val bundleJdkTag = bundleJdkVersion.replace("+", "%2B")
val bundleJdkSuffix = bundleJdkVersion.replace("+", "_")
val temurinBase = "https://github.com/adoptium/temurin21-binaries/releases/download/jdk-$bundleJdkTag"

dependencies {
    implementation(project(":core"))
    implementation("com.badlogicgames.gdx:gdx-backend-lwjgl3:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-desktop")
}

application {
    mainClass.set("com.crowdmasterarcade.desktop.DesktopLauncherKt")
}

tasks.named<Jar>("jar") {
    archiveFileName.set("$appSlug-$bundleVersion.jar")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) }
    })
    exclude("META-INF/INDEX.LIST", "META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
    manifest {
        attributes(
            "Main-Class" to application.mainClass.get(),
            "Enable-Native-Access" to "ALL-UNNAMED"
        )
    }
}

tasks.named<JavaExec>("run") {
    System.getProperty("levels.dir")?.let {
        systemProperty("levels.dir", it)
    }
}

fun downloadAndExtractJdk(url: String, targetDir: File) {
    if (targetDir.exists() && targetDir.listFiles()?.isNotEmpty() == true) return
    targetDir.mkdirs()
    val archive = targetDir.resolve(url.substringAfterLast('/'))
    if (!archive.exists()) {
        ant.invokeMethod("get", mapOf("src" to url, "dest" to archive, "skipexisting" to true, "verbose" to true))
    }
    if (archive.name.endsWith(".zip")) {
        ant.invokeMethod("unzip", mapOf("src" to archive, "dest" to targetDir))
    } else {
        ant.invokeMethod("untar", mapOf("src" to archive, "dest" to targetDir, "compression" to "gzip"))
    }
}

fun resolveJdkRoot(cacheDir: File): File {
    val javaBin = cacheDir.walkTopDown().firstOrNull {
        it.isFile && (it.name == "java" || it.name == "java.exe")
    } ?: return cacheDir
    return javaBin.parentFile.parentFile
}

val fetchJdkLinux by tasks.registering {
    val url = "$temurinBase/OpenJDK21U-jre_x64_linux_hotspot_$bundleJdkSuffix.tar.gz"
    val target = jdkCacheDir.map { it.dir("linux-x64") }
    outputs.dir(target)
    doLast { downloadAndExtractJdk(url, target.get().asFile) }
}

val fetchJdkWin by tasks.registering {
    val url = "$temurinBase/OpenJDK21U-jre_x64_windows_hotspot_$bundleJdkSuffix.zip"
    val target = jdkCacheDir.map { it.dir("win-x64") }
    outputs.dir(target)
    doLast { downloadAndExtractJdk(url, target.get().asFile) }
}

val fetchJdkMacArm64 by tasks.registering {
    val url = "$temurinBase/OpenJDK21U-jre_aarch64_mac_hotspot_$bundleJdkSuffix.tar.gz"
    val target = jdkCacheDir.map { it.dir("mac-arm64") }
    outputs.dir(target)
    doLast { downloadAndExtractJdk(url, target.get().asFile) }
}

val generateUnixLauncher by tasks.registering {
    val output = launcherDir.map { it.file(appSlug) }
    outputs.file(output)
    doLast {
        val file = output.get().asFile
        file.parentFile.mkdirs()
        file.writeText(
            """
            |#!/bin/sh
            |set -eu
            |APP_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
            |JAVA_BIN="${'$'}APP_DIR/jre/bin/java"
            |if [ ! -x "${'$'}JAVA_BIN" ]; then
            |  JAVA_BIN="java"
            |fi
            |exec "${'$'}JAVA_BIN" -Xms256m -Xmx1g -jar "${'$'}APP_DIR/$appSlug.jar" "${'$'}@"
            |
            """.trimMargin()
        )
        file.setExecutable(true, false)
    }
}

val generateWindowsLauncher by tasks.registering {
    val output = launcherDir.map { it.file("$appSlug.cmd") }
    outputs.file(output)
    doLast {
        val file = output.get().asFile
        file.parentFile.mkdirs()
        file.writeText(
            """
            |@echo off
            |setlocal
            |set "APP_DIR=%~dp0"
            |set "JAVA_BIN=%APP_DIR%jre\bin\java.exe"
            |if not exist "%JAVA_BIN%" set "JAVA_BIN=java"
            |"%JAVA_BIN%" -Xms256m -Xmx1g -jar "%APP_DIR%$appSlug.jar" %*
            |exit /b %ERRORLEVEL%
            |
            """.trimMargin()
        )
    }
}

fun CopySpec.desktopPayload(jdkCache: Provider<Directory>, includeUnixLauncher: Boolean, includeWindowsLauncher: Boolean) {
    from(tasks.named<Jar>("jar").flatMap { it.archiveFile }) {
        rename { "$appSlug.jar" }
    }
    from(rootProject.layout.projectDirectory.file("README.md")) {
        into(".")
    }
    from(rootProject.layout.projectDirectory.dir("core/src/main/resources")) {
        into("resources")
    }
    from({ resolveJdkRoot(jdkCache.get().asFile) }) {
        into("jre")
        exclude("*.zip", "*.tar.gz", "*.tar")
    }
    if (includeUnixLauncher) {
        from(generateUnixLauncher.flatMap { launcherDir.map { dir -> dir.file(appSlug) } })
    }
    if (includeWindowsLauncher) {
        from(generateWindowsLauncher.flatMap { launcherDir.map { dir -> dir.file("$appSlug.cmd") } })
    }
}

val bundleLinuxDir by tasks.registering(Sync::class) {
    dependsOn("jar", fetchJdkLinux, generateUnixLauncher)
    into(distDir.dir("$appSlug-dist-$bundleVersion-linux"))
    desktopPayload(jdkCacheDir.map { it.dir("linux-x64") }, includeUnixLauncher = true, includeWindowsLauncher = false)
}

val bundleWinDir by tasks.registering(Sync::class) {
    dependsOn("jar", fetchJdkWin, generateWindowsLauncher)
    into(distDir.dir("$appSlug-dist-$bundleVersion-win"))
    desktopPayload(jdkCacheDir.map { it.dir("win-x64") }, includeUnixLauncher = false, includeWindowsLauncher = true)
}

val bundleMacArm64Dir by tasks.registering(Sync::class) {
    dependsOn("jar", fetchJdkMacArm64, generateUnixLauncher)
    into(distDir.dir("$appSlug-dist-$bundleVersion-mac"))
    desktopPayload(jdkCacheDir.map { it.dir("mac-arm64") }, includeUnixLauncher = true, includeWindowsLauncher = false)
}

tasks.register<Tar>("bundleLinuxTar") {
    dependsOn(bundleLinuxDir)
    destinationDirectory.set(distDir)
    archiveFileName.set("$appSlug-dist-$bundleVersion-linux.tar.gz")
    compression = Compression.GZIP
    from(bundleLinuxDir.map { it.destinationDir })
}

tasks.register<Tar>("bundleMacArm64Tar") {
    dependsOn(bundleMacArm64Dir)
    destinationDirectory.set(distDir)
    archiveFileName.set("$appSlug-dist-$bundleVersion-mac.tar.gz")
    compression = Compression.GZIP
    from(bundleMacArm64Dir.map { it.destinationDir })
}

tasks.register<Zip>("bundleWin") {
    dependsOn(bundleWinDir)
    destinationDirectory.set(distDir)
    archiveFileName.set("$appSlug-dist-$bundleVersion-win.zip")
    from(bundleWinDir.map { it.destinationDir })
}

tasks.register("bundleLinuxAppImage") {
    dependsOn(bundleLinuxDir)
    outputs.file(distDir.file("$appSlug-dist-$bundleVersion-linux.AppImage"))
    doLast {
        val appImageTool = System.getenv("APPIMAGETOOL") ?: "appimagetool"
        val bundleDir = distDir.dir("$appSlug-dist-$bundleVersion-linux").asFile
        val appDir = layout.buildDirectory.dir("appimage/$appDisplayName.AppDir").get().asFile
        val outFile = distDir.file("$appSlug-dist-$bundleVersion-linux.AppImage").asFile
        delete(appDir, outFile)
        val payload = appDir.resolve("usr/lib/$appSlug")
        val bin = appDir.resolve("usr/bin")
        val applications = appDir.resolve("usr/share/applications")
        val iconDir = appDir.resolve("usr/share/icons/hicolor/512x512/apps")
        listOf(payload, bin, applications, iconDir).forEach { it.mkdirs() }
        copy { from(bundleDir); into(payload) }
        bin.resolve(appSlug).writeText("#!/bin/sh\nexec \"${'$'}APPDIR/usr/lib/$appSlug/$appSlug\" \"${'$'}@\"\n")
        bin.resolve(appSlug).setExecutable(true, false)
        appDir.resolve("AppRun").writeText("#!/bin/sh\nexec \"${'$'}APPDIR/usr/bin/$appSlug\" \"${'$'}@\"\n")
        appDir.resolve("AppRun").setExecutable(true, false)
        val desktop = """
            |[Desktop Entry]
            |Name=$appDisplayName
            |Comment=Arcade crowd runner
            |Exec=$appSlug
            |Icon=$appSlug
            |Terminal=false
            |Type=Application
            |Categories=Game;ArcadeGame;
            |
        """.trimMargin()
        appDir.resolve("$appSlug.desktop").writeText(desktop)
        applications.resolve("$appSlug.desktop").writeText(desktop)
        copy {
            from(rootProject.layout.projectDirectory.file("assets/appicon.png"))
            into(appDir)
            rename { "$appSlug.png" }
        }
        copy {
            from(rootProject.layout.projectDirectory.file("assets/appicon.png"))
            into(iconDir)
            rename { "$appSlug.png" }
        }
        val process = ProcessBuilder(appImageTool, appDir.absolutePath, outFile.absolutePath)
            .directory(rootProject.projectDir)
            .inheritIO()
        process.environment()["ARCH"] = "x86_64"
        if (appImageTool.endsWith(".AppImage")) {
            process.environment()["APPIMAGE_EXTRACT_AND_RUN"] = "1"
        }
        if (process.start().waitFor() != 0) {
            throw GradleException("appimagetool failed while building ${outFile.name}.")
        }
    }
}

tasks.register("bundleLinuxSnap") {
    outputs.file(distDir.file("$appSlug-dist-$bundleVersion-linux.snap"))
    doLast {
        val process = ProcessBuilder("snapcraft", "--destructive-mode")
            .directory(rootProject.projectDir)
            .inheritIO()
        process.environment()["CROWD_MASTER_ARCADE_SNAP_VERSION"] = bundleVersion
        if (process.start().waitFor() != 0) {
            throw GradleException("snapcraft failed.")
        }
        val built = rootProject.layout.projectDirectory.asFile
            .listFiles()
            ?.filter { it.extension == "snap" }
            ?.maxByOrNull { it.lastModified() }
            ?: error("Snap artifact not found after snapcraft build.")
        copy {
            from(built)
            into(distDir)
            rename { "$appSlug-dist-$bundleVersion-linux.snap" }
        }
    }
}

tasks.register("bundleAll") {
    dependsOn("bundleLinuxTar", "bundleWin", "bundleMacArm64Tar")
}

tasks.register("bundleLocalDir") {
    val osName = System.getProperty("os.name").lowercase()
    dependsOn(
        when {
            osName.contains("win") -> "bundleWinDir"
            osName.contains("mac") -> "bundleMacArm64Dir"
            else -> "bundleLinuxDir"
        }
    )
}

tasks.register("dev") {
    dependsOn("bundleLocalDir")
}
