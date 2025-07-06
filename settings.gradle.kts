plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
rootProject.name = "pasla"
include("src:main:java")
findProject(":src:main:java")?.name = "java"
