plugins {
    kotlin("jvm") version "2.0.20"
    application
}

group = "io.github.supchik22"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()

}

val lwjglVersion = "3.3.3"

dependencies {
    testImplementation(kotlin("test"))

    // LWJGL core
    implementation("org.lwjgl:lwjgl:$lwjglVersion")
    implementation("org.lwjgl:lwjgl-glfw:$lwjglVersion")
    implementation("org.lwjgl:lwjgl-opengl:$lwjglVersion")

    // LWJGL natives (для Windows)
    runtimeOnly("org.lwjgl:lwjgl:$lwjglVersion:natives-windows")
    runtimeOnly("org.lwjgl:lwjgl-glfw:$lwjglVersion:natives-windows")
    runtimeOnly("org.lwjgl:lwjgl-opengl:$lwjglVersion:natives-windows")

    implementation("org.lwjgl:lwjgl-stb:$lwjglVersion")
    runtimeOnly("org.lwjgl:lwjgl-stb:$lwjglVersion:natives-windows")

    // JOML для векторів/матриць
    implementation("org.joml:joml:1.10.5")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(19)
}

application {

    mainClass.set("io.github.supchik22.MainKt")
}
