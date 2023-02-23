plugins {
    kotlin("jvm") version "1.8.0"
    kotlin("plugin.serialization") version "1.8.0"
    id("org.jlleitschuh.gradle.ktlint") version "11.2.0"
    application
}

group = "io.github.zabuzard.discordplays"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation("io.github.oshai:kotlin-logging-jvm:4.0.0-beta-22")
    implementation("org.slf4j:slf4j-api:2.0.6")
    implementation("org.apache.logging.log4j:log4j-api:2.20.0")
    implementation("org.apache.logging.log4j:log4j-core:2.20.0")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.20.0")

    implementation("me.jakejmattson:DiscordKt:0.23.4")
    implementation("com.github.trekawek:coffee-gb:master-SNAPSHOT")
    implementation("com.sksamuel.aedile:aedile-core:1.2.0")

    testImplementation(kotlin("test"))
}

configurations.implementation {
    exclude("org.slf4j", "slf4j-simple")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(18)
}

application {
    mainClass.set("io.github.zabuzard.discordplays.MainKt")
}
