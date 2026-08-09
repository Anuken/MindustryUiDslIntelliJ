import org.jetbrains.kotlin.gradle.tasks.*

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.25" //2.x makes the plugin explode
    id("org.jetbrains.intellij") version "1.17.4"
}

group = "com.anuke.mindustry.uidsl"
version = "0.0.4"

repositories {
    mavenCentral()
}

intellij {
    version.set("2024.1")
    type.set("IC") //IntelliJ IDEA Community Edition
    plugins.set(listOf())
    updateSinceUntilBuild.set(false)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs = freeCompilerArgs + "-Xjvm-default=all"
    }
}

tasks {
    patchPluginXml {
        sinceBuild.set("241")
        untilBuild.set("262.*") //likely won't work, but it's worth a try
    }

    runIde {
        jvmArgs("-Xmx2g")
    }
}
