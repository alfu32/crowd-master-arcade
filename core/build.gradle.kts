plugins {
    kotlin("jvm")
}

val gdxVersion: String by project

dependencies {
    api("com.badlogicgames.gdx:gdx:$gdxVersion")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
