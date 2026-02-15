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

package cn.enaium.fabric.gradle.utility

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.internal.tasks.JvmConstants
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.language.jvm.tasks.ProcessResources

/**
 * @author Enaium
 */
internal operator fun <T : Any> NamedDomainObjectProvider<T>.invoke(action: T.() -> Unit) {
    configure(action)
}

internal fun DependencyHandler.implementation(dependency: Any) {
    add(JvmConstants.IMPLEMENTATION_CONFIGURATION_NAME, dependency)
}

internal fun DependencyHandler.minecraft(dependency: Any) {
    add("minecraft", dependency)
}

internal fun DependencyHandler.modImplementation(dependency: Any) {
    add("modImplementation", dependency)
}

internal fun DependencyHandler.mappings(dependency: Any) {
    add("mappings", dependency)
}

internal val TaskContainer.processResources: TaskProvider<ProcessResources>
    get() = named("processResources", ProcessResources::class.java)

internal val Project.sourceSets: SourceSetContainer
    get() = (this as ExtensionAware).extensions.getByName("sourceSets") as SourceSetContainer

internal val SourceSetContainer.main: NamedDomainObjectProvider<SourceSet>
    get() = named("main", SourceSet::class.java)

internal val NamedDomainObjectContainer<Configuration>.runtimeClasspath: NamedDomainObjectProvider<Configuration>
    get() = named("runtimeClasspath", Configuration::class.java)
