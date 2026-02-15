# fabric-multi-game

This plugin makes you not write more code for fabric multi game project.

## Usage

### Parent Project

`build.gradle.kts`

```kotlin
plugins {
    id("cn.enaium.fabric-multi-game")
}

fmg {
    common.set(project(":core"))
}
```

### Children Projects

`gradle.properties`

```properties
java.version=21
minecraft.version=1.21.11
fabric.yarn.version=1.21.11+build.3
fabric.loader.version=0.18.4
fabric.api.version=0.139.5+1.21.11
```