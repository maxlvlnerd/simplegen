plugins {
    id("java")
}

group = "com.github.maxlvlnerd"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("net.minestom:minestom-snapshots:12794d4263")
    implementation(project(":"))

    runtimeOnly("org.slf4j:slf4j-simple:2.1.0-alpha1")
}