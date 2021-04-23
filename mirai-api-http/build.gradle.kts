import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
plugins {
    id("kotlinx-serialization")
    id("com.github.johnrengelman.shadow") version "6.1.0"
    kotlin("jvm")
}

val httpVersion: String by rootProject.ext

val ktorVersion: String by rootProject.ext
val serializationVersion: String by rootProject.ext

fun kotlinx(id: String, version: String) =
    "org.jetbrains.kotlinx:kotlinx-$id:$version"


fun ktor(id: String, version: String = this@Build_gradle.ktorVersion) = "io.ktor:ktor-$id:$version"

val miraiVersion = "2.5.2"
kotlin {
    sourceSets["test"].apply {
        dependencies {
            api("org.slf4j:slf4j-simple:1.7.26")
        }
    }

    sourceSets.all {
        languageSettings.enableLanguageFeature("InlineClasses")
        languageSettings.useExperimentalAnnotation("kotlin.Experimental")

        dependencies {
            api(kotlinx("serialization-json", serializationVersion))
            implementation("net.mamoe:mirai-core-utils:${miraiVersion}")
            api("net.mamoe:mirai-core:$miraiVersion")
            api(ktor("server-cio"))
            api(ktor("http-jvm"))
            api(ktor("websockets"))
            implementation("net.mamoe.yamlkt:yamlkt:0.9.0")
            implementation(ktor("server-core"))
            implementation(ktor("http"))
        }
    }
}

project.version = httpVersion

description = "Mirai HTTP API plugin"

/*internal val EXCLUDED_FILES = listOf(
    "kotlin-stdlib-.*",
    "kotlin-reflect-.*",
    "kotlinx-serialization-json.*",
    "kotlinx-coroutines.*",
    "kotlinx-serialization-core.*",
    "slf4j-api.*"
).map { "^$it\$".toRegex() }*/

val compileKotlin: KotlinCompile by tasks

compileKotlin.kotlinOptions.jvmTarget="1.8"
tasks {
    named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
        this.manifest {
            this.attributes(
                "Main-Class" to "net.mamoe.mirai.api.http.HttpApi"
            )
        }
     /*   this.exclude { elm ->
            EXCLUDED_FILES.any { it.matches(elm.path) }
        }*/
    }
}
tasks.create("buildCiJar", Jar::class) {
    dependsOn("buildPlugin")
    doLast {
        val buildPluginTask = tasks.getByName("buildPlugin", Jar::class)
        val buildPluginFile = buildPluginTask.archiveFile.get().asFile
        project.buildDir.resolve("ci").also {
            it.mkdirs()
        }.resolve("mirai-api-http.jar").let {
            buildPluginFile.copyTo(it, true)
        }
    }
}

