import de.undercouch.gradle.tasks.download.Download
import java.io.OutputStream.nullOutputStream

plugins {
    kotlin("jvm") version "1.5.20"
    kotlin("plugin.serialization") version "1.5.20"
    id("com.github.johnrengelman.shadow") version "7.0.0"
    `maven-publish`
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(16))
    }
}

repositories {
    mavenCentral()
    maven(url = "https://papermc.io/repo/repository/maven-public/")
    maven(url = "https://jitpack.io/")
    mavenLocal()
}

dependencies {
    compileOnly(kotlin("stdlib"))
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.0")
    compileOnly("io.papermc.paper:paper-api:1.17-R0.1-SNAPSHOT")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.0")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.7.0")
    testImplementation("org.mockito:mockito-core:3.6.28")
    testImplementation("org.spigotmc:spigot:1.17-R0.1-SNAPSHOT")
}

tasks {
    withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release.set(16)
    }

    processResources {
        filesMatching("**/*.yml") {
            expand(project.properties)
        }
    }

    test {
        useJUnitPlatform()
        doLast {
            file("logs").deleteRecursively()
        }
    }

    create<Jar>("paperJar") {
        from(sourceSets["main"].output)
        archiveBaseName.set(project.property("pluginName").toString())
        archiveVersion.set("") // For bukkit plugin update

        doLast {
            var dest = File(rootDir, ".debug/plugins")
            val pluginName = archiveFileName.get()
            val pluginFile = File(dest, pluginName)
            if (pluginFile.exists()) dest = File(dest, "update")

            copy {
                from(archiveFile)
                into(dest)
            }
        }
    }

    create<Jar>("sourcesJar") {
        from(sourceSets["main"].allSource)
        archiveClassifier.set("sources")
    }

    create<DefaultTask>("setupWorkspace") {
        doLast {
            val versions = arrayOf(
                "1.17"
            )
            val buildtoolsDir = file(".buildtools")
            val buildtools = File(buildtoolsDir, "BuildTools.jar")

            val maven = File(System.getProperty("user.home"), ".m2/repository/org/spigotmc/spigot/")
            val repos = maven.listFiles { file: File -> file.isDirectory } ?: emptyArray()
            val missingVersions = versions.filter { version ->
                repos.find { it.name.startsWith(version) }?.also { println("Skip downloading spigot-$version") } == null
            }.also { if (it.isEmpty()) return@doLast }

            val download by registering(Download::class) {
                src("https://hub.spigotmc.org/jenkins/job/BuildTools/lastSuccessfulBuild/artifact/target/BuildTools.jar")
                dest(buildtools)
            }
            download.get().download()

            runCatching {
                for (v in missingVersions) {
                    println("Downloading spigot-$v...")

                    javaexec {
                        workingDir(buildtoolsDir)
                        mainClass.set("-jar")
                        args = listOf("./${buildtools.name}", "--rev", v)
                        // Silent
                        standardOutput = nullOutputStream()
                        errorOutput = nullOutputStream()
                    }
                }
            }.onFailure {
                it.printStackTrace()
            }
            buildtoolsDir.deleteRecursively()
        }
    }
}

publishing {
    publications {
        create<MavenPublication>(project.property("pluginName").toString()) {
            artifactId = project.name
            from(components["java"])
            artifact(tasks["sourcesJar"])
        }
    }
}