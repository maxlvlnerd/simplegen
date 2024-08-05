plugins {
    id("java")
    id("maven-publish")
}

group = "com.github.maxlvlnerd"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("net.minestom:minestom-snapshots:12794d4263")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}
java.toolchain {
    languageVersion.set(JavaLanguageVersion.of(21))
}
tasks.test {
    useJUnitPlatform()
}
publishing {
    publications {
        create<MavenPublication>("simplegen") {
            from(components["java"])
        }
    }
}