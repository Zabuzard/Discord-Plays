plugins {
    application
    kotlin("jvm") version "1.8.0"
    kotlin("plugin.serialization") version "1.8.0"
    id("org.jlleitschuh.gradle.ktlint") version "11.2.0"
}

group = "io.github.zabuzard.discordplays"
version = "2.4-SNAPSHOT"

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

    implementation("dev.kord:kord-core:0.8.0-M17")
    implementation("dev.kord.x:emoji:0.5.0")
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

tasks {
    val fatJar = register<Jar>("fatJar") {
        dependsOn.addAll(listOf("compileJava", "compileKotlin", "processResources"))
        archiveClassifier.set("standalone")
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        manifest { attributes(mapOf("Main-Class" to application.mainClass)) }
        val sourcesMain = sourceSets.main.get()
        val contents = configurations.runtimeClasspath.get()
            .map { if (it.isDirectory) it else zipTree(it) } +
            sourcesMain.output
        from(contents)
    }
    build {
        dependsOn(fatJar)
    }
}
