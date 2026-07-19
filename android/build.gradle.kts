plugins {
    id("com.android.application")
}

val gdxVersion: String by project
val releaseVersionName = (findProperty("releaseNumber") ?: rootProject.version).toString()
val releaseVersionCode = (
    findProperty("androidVersionCode")
        ?: System.getenv("ANDROID_VERSION_CODE")
        ?: "1"
    ).toString().toInt()
val releaseStoreFile = (findProperty("ANDROID_UPLOAD_STORE_FILE") ?: System.getenv("ANDROID_UPLOAD_STORE_FILE"))?.toString()
val releaseStorePassword = (findProperty("ANDROID_UPLOAD_STORE_PASSWORD") ?: System.getenv("ANDROID_UPLOAD_STORE_PASSWORD"))?.toString()
val releaseKeyAlias = (findProperty("ANDROID_UPLOAD_KEY_ALIAS") ?: System.getenv("ANDROID_UPLOAD_KEY_ALIAS"))?.toString()
val releaseKeyPassword = (findProperty("ANDROID_UPLOAD_KEY_PASSWORD") ?: System.getenv("ANDROID_UPLOAD_KEY_PASSWORD"))?.toString()
val hasReleaseSigning = releaseStoreFile != null &&
    releaseStorePassword != null &&
    releaseKeyAlias != null &&
    releaseKeyPassword != null

configurations {
    create("natives")
}

android {
    namespace = "com.crowdmasterarcade"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.crowdmasterarcade"
        minSdk = 26
        targetSdk = 35
        versionCode = releaseVersionCode
        versionName = releaseVersionName
        multiDexEnabled = true
    }

    signingConfigs {
        create("release") {
            if (hasReleaseSigning) {
                storeFile = file(releaseStoreFile!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        getByName("debug") {
            isMinifyEnabled = false
        }
        getByName("release") {
            isMinifyEnabled = false
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    sourceSets {
        getByName("main") {
            manifest.srcFile("src/main/AndroidManifest.xml")
            java.srcDirs("src/main/kotlin")
            assets.srcDirs(rootProject.file("core/src/main/resources"))
            res.srcDirs("src/main/res")
            jniLibs.srcDirs("${layout.buildDirectory.get().asFile}/androidNatives")
        }
    }

    androidResources {
        ignoreAssetsPatterns.add("!*.bak")
    }

    packaging {
        resources {
            excludes += setOf(
                "META-INF/INDEX.LIST",
                "META-INF/*.SF",
                "META-INF/*.DSA",
                "META-INF/*.RSA"
            )
        }
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}

val copyAndroidNatives by tasks.registering {
    doLast {
        val outDir = layout.buildDirectory.dir("androidNatives").get().asFile
        delete(outDir)
        outDir.mkdirs()
        configurations.getByName("natives").files.forEach { jarFile ->
            val abi = when {
                jarFile.name.contains("natives-arm64-v8a") -> "arm64-v8a"
                jarFile.name.contains("natives-armeabi-v7a") -> "armeabi-v7a"
                jarFile.name.contains("natives-x86_64") -> "x86_64"
                jarFile.name.contains("natives-x86") -> "x86"
                else -> null
            } ?: return@forEach
            copy {
                from(zipTree(jarFile))
                include("**/*.so")
                into(outDir.resolve(abi))
                includeEmptyDirs = false
                eachFile { path = name }
            }
        }
    }
}

tasks.matching { it.name == "preBuild" }.configureEach {
    dependsOn(copyAndroidNatives)
}

tasks.matching { it.name.startsWith("merge") && it.name.endsWith("Assets") }.configureEach {
    dependsOn(":core:generateAssetIndex")
}

tasks.register<Copy>("bundleAndroidApk") {
    dependsOn("assembleRelease")
    from(layout.buildDirectory.dir("outputs/apk/release"))
    include("*-release.apk")
    rename { "crowd-master-arcade-$releaseVersionName.apk" }
    into(rootProject.layout.projectDirectory.dir("dist"))
}

tasks.register<Copy>("bundleAndroidDebugApk") {
    dependsOn("assembleDebug")
    from(layout.buildDirectory.dir("outputs/apk/debug"))
    include("*-debug.apk")
    rename { "crowd-master-arcade-$releaseVersionName-debug.apk" }
    into(rootProject.layout.projectDirectory.dir("dist"))
}

tasks.register("bundleAndroidLocalTestApk") {
    group = "distribution"
    description = "Builds a debug APK for local device testing and copies it to dist/."
    dependsOn("bundleAndroidDebugApk")
}

tasks.register<Copy>("bundleAndroidAab") {
    dependsOn("bundleRelease")
    from(layout.buildDirectory.dir("outputs/bundle/release"))
    include("*.aab")
    rename { "crowd-master-arcade-$releaseVersionName.aab" }
    into(rootProject.layout.projectDirectory.dir("dist"))
}

gradle.taskGraph.whenReady {
    val needsReleaseSigning = hasTask(":android:bundleRelease") ||
        hasTask(":android:assembleRelease") ||
        hasTask(":android:bundleAndroidApk") ||
        hasTask(":android:bundleAndroidAab")
    if (needsReleaseSigning && !hasReleaseSigning) {
        throw GradleException(
            "Release signing is not configured. Set ANDROID_UPLOAD_STORE_FILE, " +
                "ANDROID_UPLOAD_STORE_PASSWORD, ANDROID_UPLOAD_KEY_ALIAS, ANDROID_UPLOAD_KEY_PASSWORD."
        )
    }
}

dependencies {
    implementation(project(":core"))
    implementation("com.badlogicgames.gdx:gdx-backend-android:$gdxVersion")
    "natives"("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-armeabi-v7a")
    "natives"("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-arm64-v8a")
    "natives"("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-x86")
    "natives"("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-x86_64")
}
