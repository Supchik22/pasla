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

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")


    implementation("org.lwjgl:lwjgl-stb:$lwjglVersion")
    runtimeOnly("org.lwjgl:lwjgl-stb:$lwjglVersion:natives-windows")

    // JOML для векторів/матриць
    implementation("org.joml:joml:1.10.5")
}


val imguiVersion = "1.89.0"
dependencies {

    implementation(platform("org.lwjgl:lwjgl-bom:$lwjglVersion"))
    // Залежності ImGui-Java
    implementation("io.github.spair:imgui-java-binding:$imguiVersion")
    implementation("io.github.spair:imgui-java-lwjgl3:$imguiVersion")

    // Нативні бібліотеки ImGui-Java для Windows
    runtimeOnly("io.github.spair:imgui-java-natives-windows:$imguiVersion")
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
