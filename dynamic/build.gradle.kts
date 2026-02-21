plugins {
    groovy
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.groovy.all)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}