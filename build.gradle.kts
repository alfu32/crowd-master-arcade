plugins {
    kotlin("jvm") version "2.2.20" apply false
    id("com.android.application") version "9.0.0" apply false
}

allprojects {
    group = "com.crowdmasterarcade"
    version = (findProperty("releaseNumber") ?: findProperty("projectVersion") ?: "0.1.0").toString()
}
