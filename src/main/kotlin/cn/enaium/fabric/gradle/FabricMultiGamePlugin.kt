/*
 * Copyright 2026 Enaium
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.enaium.fabric.gradle

import cn.enaium.fabric.gradle.extension.FabricMultiGameExtension
import cn.enaium.fabric.gradle.utility.*
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.BasePluginExtension
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.bundling.Jar
import org.gradle.util.internal.VersionNumber
import tools.jackson.databind.node.ObjectNode
import tools.jackson.module.kotlin.jacksonObjectMapper
import tools.jackson.module.kotlin.readValue

/**
 * @author Enaium
 */
class FabricMultiGamePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("fmg", FabricMultiGameExtension::class.java)
        project.afterEvaluate { afterEvaluate ->
            afterEvaluate.subprojects { subproject ->
                subproject.repositories.maven { it.setUrl("https://maven.fabricmc.net/") }
                subproject.repositories.maven { it.setUrl("https://repo.legacyfabric.net/repository/legacyfabric/") }
                val minecraftVersion = subproject.properties["minecraft.version"].toString()
                val loaderVersion = subproject.properties["fabric.loader.version"].toString()
                val apiVersion = subproject.properties["fabric.api.version"].toString()
                val javaVersion = subproject.properties["java.version"].toString()
                val modern = VersionNumber.parse(minecraftVersion) >= VersionNumber.parse("1.14")
                val disableObfuscation =
                    subproject.properties.getOrDefault("fabric.loom.disableObfuscation", false).toString().toBoolean()
                subproject.plugins.apply("java")
                subproject.plugins.apply("fabric-loom")
                if (!modern) {
                    subproject.plugins.apply("legacy-looming")
                }

                hasClass("org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension")?.also {
                    subproject.plugins.apply("org.jetbrains.kotlin.jvm")
                    KotlinExtension.jvmToolchain(subproject.extensions.getByName("kotlin"), javaVersion.toInt())
                }

                subproject.dependencies.minecraft("com.mojang:minecraft:$minecraftVersion")
                if (disableObfuscation) {
                    subproject.dependencies.implementation("net.fabricmc:fabric-loader:$loaderVersion")
                } else {
                    subproject.dependencies.modImplementation("net.fabricmc:fabric-loader:$loaderVersion")
                }

                subproject.properties.getOrDefault("fabric.yarn.version", null)?.also {
                    subproject.dependencies.mappings("${if (modern) "net.fabricmc:yarn" else "net.legacyfabric:yarn"}:$it:v2")
                }
                if (disableObfuscation) {
                    subproject.dependencies.implementation("${if (modern) "net.fabricmc.fabric-api:fabric-api" else "net.legacyfabric.legacy-fabric-api:legacy-fabric-api"}:$apiVersion")
                } else {
                    subproject.dependencies.modImplementation("${if (modern) "net.fabricmc.fabric-api:fabric-api" else "net.legacyfabric.legacy-fabric-api:legacy-fabric-api"}:$apiVersion")
                }
                val core = extension.common.get()
                core.tasks.named("jar", Jar::class.java) {
                    it.exclude("fabric.mod.json")
                    it.exclude("*.mixins.json")
                }
                subproject.extensions.getByType(BasePluginExtension::class.java).archivesName.set(project.rootProject.name)
                subproject.dependencies.implementation(core)
                subproject.version = "${minecraftVersion}-${project.rootProject.version}"
                subproject.tasks.processResources {
                    from(core.sourceSets.main.get().output)
                    inputs.property("currentTimeMillis", System.currentTimeMillis())

                    eachFile { origin ->
                        val output =
                            subproject.layout.projectDirectory.asFile.resolve(origin.file.relativeTo(core.layout.projectDirectory.asFile))
                        if (origin.name.endsWith("mixins.json")) {
                            origin.exclude()
                            val mapper = jacksonObjectMapper()
                            val mixins = mapper.readValue<ObjectNode>(origin.file.readText())
                            val pn = mixins["package"].asString()
                            mixins.set("mixins", mapper.createArrayNode().apply {
                                subproject.file("src/main/java/${pn.replace(".", "/")}").listFiles()
                                    .forEach { add(it.name.substringBeforeLast(".")) }
                            })
                            mixins.put(
                                "compatibilityLevel",
                                "JAVA_${javaVersion}"
                            )
                            output.writeText(mapper.writeValueAsString(mixins))
                        } else if (origin.name == "fabric.mod.json") {
                            origin.exclude()
                            var modified = origin.file.readText()
                            val map = mapOf(
                                "version" to subproject.version.toString(),
                                "minecraft_version" to minecraftVersion
                                    .let { "${it.subSequence(0, it.lastIndexOf("."))}.x" },
                                "java_version" to javaVersion,
                            )
                            map.forEach { (k, v) ->
                                modified = modified.replace("@$k@", v)
                            }
                            output.writeText(modified)
                        }
                    }
                }

                subproject.extensions.getByType(JavaPluginExtension::class.java).apply {
                    sourceCompatibility = JavaVersion.toVersion(javaVersion.toInt())
                    targetCompatibility = JavaVersion.toVersion(javaVersion.toInt())
                }

                subproject.afterEvaluate { after ->
                    after.configurations.runtimeClasspath.get().forEach { file ->
                        if (file.name.startsWith("sponge-mixin")) {
                            after.tasks.named("runClient", JavaExec::class.java) {
                                it.jvmArgs("-javaagent:${file.absolutePath}")
                            }
                            after.tasks.named("runServer", JavaExec::class.java) {
                                it.jvmArgs("-javaagent:${file.absolutePath}")
                            }
                        }
                    }
                }
            }
        }
    }
}