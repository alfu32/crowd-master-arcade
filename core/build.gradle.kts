plugins {
    kotlin("jvm")
}

val gdxVersion: String by project

dependencies {
    api("com.badlogicgames.gdx:gdx:$gdxVersion")
    api("com.kotcrab.vis:vis-ui:1.5.3")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
