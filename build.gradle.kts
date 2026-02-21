plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.gradle.publish)
    `java-gradle-plugin`
}

group = "cn.enaium"
version = providers.gradleProperty("version").get()

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(project("dynamic"))
    implementation(libs.jackson)
    testImplementation(kotlin("test"))
}

afterEvaluate {
    tasks.processResources {
        from(rootProject.project("dynamic").sourceSets.main.get().output)
    }
}

kotlin {
    jvmToolchain(17)
}

tasks.test {
    useJUnitPlatform()
}

gradlePlugin {
    website.set("https://github.com/Enaium/fabric-multi-game/")
    vcsUrl.set("https://github.com/Enaium/fabric-multi-game/")
    plugins {
        create("fmg") {
            id = "cn.enaium.fabric-multi-game"
            implementationClass = "cn.enaium.fabric.gradle.FabricMultiGamePlugin"
            displayName = "fabric-multi-game"
            description = "This plugin makes you not write more code for fabric multi game project."
            tags.set(listOf("fabric", "minecraft", "mod"))
        }
    }
}

allprojects {
    apply(plugin = "java")
    apply(plugin = "maven-publish")

    publishing {
        publications {
            create<MavenPublication>("maven") {
                groupId = project.group.toString()
                artifactId = project.name
                version = project.version.toString()

                from(components["java"])
            }
        }
    }
}