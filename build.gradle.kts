import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.25"
    id("org.jetbrains.intellij") version "1.17.4"
}

group = "com.anuke.mindustry.uidsl"
version = "0.0.3"

repositories {
    mavenCentral()
}

intellij {
    version.set("2024.1")
    type.set("IC") // IntelliJ IDEA Community Edition
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
        untilBuild.set("262.*")
    }

    // The bundled schema.json / icon live under src/main/resources already;
    // nothing extra to wire up here.

    runIde {
        // Opens a sandbox IDE with the plugin installed, pre-loaded with the sample file.
        jvmArgs("-Xmx2g")
    }
}
